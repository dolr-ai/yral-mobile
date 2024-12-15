//
//  AuthClient.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import secp256k1

class DefaultAuthClient: AuthClient {
  private(set) var identity: DelegatedIdentity?
  private(set) var principal: Principal?
  private let networkService: NetworkService
  private let cookieStorage = HTTPCookieStorage.shared

  init(networkService: NetworkService) {
    self.networkService = networkService
  }

  func initialize() async throws {
    if let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) {
      try await refreshAuthIfNeeded(using: existingCookie)
    } else {
      try await fetchAndSetAuthCookie()
    }
  }

  func refreshAuthIfNeeded() async throws {
    guard let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
      try await fetchAndSetAuthCookie()
      return
    }

    if let expiresDate = existingCookie.expiresDate, expiresDate < Date() {
      try await fetchAndSetAuthCookie()
    } else {
      try await extractIdentity(from: existingCookie)
    }
  }

  private func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws {
    if let expiresDate = cookie.expiresDate, expiresDate < Date() {
      try await fetchAndSetAuthCookie()
    } else {
      try await extractIdentity(from: cookie)
    }
  }

  private func fetchAndSetAuthCookie() async throws {
    let payload = try createAuthPayload()
    let endpoint = AuthEndpoints.setAnonymousIdentityCookie(payload: payload)

    _ = try await networkService.performRequest(for: endpoint)

    guard let newCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
      throw NetworkError.invalidResponse("Failed to fetch cookie")
    }
    try await extractIdentity(from: newCookie)
  }

  private func extractIdentity(from cookie: HTTPCookie) async throws {
    let endpoint = AuthEndpoints.extractIdentity(cookie: cookie)

    let data = try await networkService.performRequest(for: endpoint)
    try await handleExtractIdentityResponse(from: data)
  }

  private func handleExtractIdentityResponse(from data: Data) async throws {
    try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
      if buffer.count > 0 {
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
        let identity = try delegated_identity_from_bytes(uint8Buffer)

        Task { @MainActor in
          self.identity = identity
          self.principal = principal
        }
      } else {
        throw NetworkError.invalidResponse("Empty data received")
      }
    }
  }

  private func createAuthPayload() throws -> Data {
    let privateKey = try secp256k1.Signing.PrivateKey(format: .uncompressed)
    let publicKeyData = privateKey.publicKey.dataRepresentation

    let xData = publicKeyData[1...32].base64URLEncodedString()
    let yData = publicKeyData[33...64].base64URLEncodedString()
    let dData = privateKey.dataRepresentation.base64URLEncodedString()

    let jwk: [String: Any] = [
      "kty": "EC",
      "crv": "secp256k1",
      "x": xData,
      "y": yData,
      "d": dData
    ]

    let payload: [String: Any] = ["anonymous_identity": jwk]
    return try JSONSerialization.data(withJSONObject: payload)
  }
}

protocol AuthClient {
  var identity: DelegatedIdentity? { get }
  var principal: Principal? { get }
  func initialize() async throws
  func refreshAuthIfNeeded() async throws
}
