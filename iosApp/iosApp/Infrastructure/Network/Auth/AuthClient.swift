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
  private(set) var principalString: String?
  private let networkService: NetworkService
  private let cookieStorage = HTTPCookieStorage.shared
  private(set) var identityData: Data?
  private let keychainIdentityKey = "yral.delegatedIdentity"

  init(networkService: NetworkService) {
    self.networkService = networkService
  }

  @MainActor func initialize() async throws {
    if let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) {
      try await refreshAuthIfNeeded(using: existingCookie)
    } else {
      try await fetchAndSetAuthCookie()
    }
  }

  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws {
    if let expiresDate = cookie.expiresDate, expiresDate < Date() {
      try await fetchAndSetAuthCookie()
    } else {
      do {
        guard let data = try KeychainHelper.retrieveData(for: keychainIdentityKey) else {
          try await extractIdentity(from: cookie)
          return
        }
        identityData = data
        try await handleExtractIdentityResponse(from: data)
      } catch {
        print(error)
        try await extractIdentity(from: cookie)
      }
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
    identityData = data
    try KeychainHelper.store(data: data, for: keychainIdentityKey)
    try await handleExtractIdentityResponse(from: data)
  }

  private func handleExtractIdentityResponse(from data: Data) async throws {
    let (wire, identity): (DelegatedIdentityWire, DelegatedIdentity) = try data.withUnsafeBytes { buffer in
      guard buffer.count > 0 else {
        throw NetworkError.invalidResponse("Empty data received")
      }

      let uint8Buffer = buffer.bindMemory(to: UInt8.self)
      let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
      let identity = try delegated_identity_from_bytes(uint8Buffer)
      return (wire, identity)
    }
    let canistersWrapper = try await authenticate_with_network(wire, nil)
    let principal = canistersWrapper.get_canister_principal()
    let principalString = canistersWrapper.get_canister_principal_string().toString()

    await MainActor.run {
      self.identity = identity
      self.principal = principal
      self.principalString = principalString
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

  func generateNewDelegatedIdentity() throws -> DelegatedIdentity {
    guard let data = identityData else {
      throw NetworkError.invalidResponse("No identity data available")
    }
    return try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
      if buffer.count > 0 {
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        return try delegated_identity_from_bytes(uint8Buffer)
      } else {
        throw NetworkError.invalidResponse("Empty data received")
      }
    }
  }

  func logout() {
    if let cookies = cookieStorage.cookies {
      for cookie in cookies where cookie.name == AuthConstants.cookieName {
        cookieStorage.deleteCookie(cookie)
      }
    }
    try? KeychainHelper.deleteItem(for: keychainIdentityKey)
    self.identity = nil
    self.principal = nil
    self.principalString = nil
    self.identityData = nil
  }
}

protocol AuthClient {
  var identity: DelegatedIdentity? { get }
  var principal: Principal? { get }
  var principalString: String? { get }
  func initialize() async throws
  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws
  func generateNewDelegatedIdentity() throws -> DelegatedIdentity
}

extension DefaultAuthClient {
  enum Constants {
    static let keychainIdentity = "yral.delegatedIdentity"
  }
}
