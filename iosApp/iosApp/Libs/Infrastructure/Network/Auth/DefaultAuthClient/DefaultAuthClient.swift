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
  private let keychainIdentityKey = Constants.keychainIdentity
  private let crashReporter: CrashReporter

  init(networkService: NetworkService, crashReporter: CrashReporter) {
    self.networkService = networkService
    self.crashReporter = crashReporter
  }

  @MainActor
  func initialize() async throws {
    try await recordThrowingOperation {
      if let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) {
        do {
          try await refreshAuthIfNeeded(using: existingCookie)
        } catch {
          crashReporter.recordException(error)
          crashReporter.log("Error received from refreshAuthIfNeeded")
          try await fetchAndSetAuthCookie()
        }
      } else {
        try await fetchAndSetAuthCookie()
      }
    }
  }

  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws {
    try await recordThrowingOperation {
      if let expiresDate = cookie.expiresDate, expiresDate < Date() {
        try await fetchAndSetAuthCookie()
      } else {
        do {
          guard let data =
                  try KeychainHelper.retrieveData(for: keychainIdentityKey),
                !data.isEmpty else {
            try await extractIdentity(from: cookie)
            return
          }
          identityData = data
          if let identityData {
            try await handleExtractIdentityResponse(from: identityData)
          }
        } catch {
          try? KeychainHelper.deleteItem(for: keychainIdentityKey)
          identityData = nil
          try await extractIdentity(from: cookie)
        }
      }
    }
  }

  func generateNewDelegatedIdentity() throws -> DelegatedIdentity {
    return try recordThrowingOperation {
      guard let data = identityData else {
        throw NetworkError.invalidResponse("No identity data available")
      }
      return try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
        if buffer.count > .zero {
          let uint8Buffer = buffer.bindMemory(to: UInt8.self)
          return try delegated_identity_from_bytes(uint8Buffer)
        } else {
          throw NetworkError.invalidResponse("Empty data received")
        }
      }
    }
  }

  func generateNewDelegatedIdentityWireOneHour() throws -> DelegatedIdentityWire {
    return try recordThrowingOperation {
      guard let parentData = identityData else {
        throw NetworkError.invalidResponse("No existing identity data available.")
      }

      let parentWire: DelegatedIdentityWire = try parentData.withUnsafeBytes { buf in
        guard buf.count > .zero else {
          throw NetworkError.invalidResponse("Empty parent identity data.")
        }
        return try delegated_identity_wire_from_bytes(buf.bindMemory(to: UInt8.self))
      }

      let ephemeralJsonData = try createAuthPayload()
      guard
        let parsed = try JSONSerialization.jsonObject(with: ephemeralJsonData) as? [String: Any],
        var anon = parsed["anonymous_identity"] as? [String: Any]
      else {
        throw NetworkError.invalidResponse("Failed to parse ephemeral JSON.")
      }

      anon.removeValue(forKey: "d") // remove the private key

      let ephemeralPublicDict: [String: Any] = [
        "kty": anon["kty"] ?? "EC",
        "crv": anon["crv"] ?? "secp256k1",
        "x": anon["x"] ?? "",
        "y": anon["y"] ?? ""
      ]
      let ephemeralPublicData = try JSONSerialization.data(withJSONObject: ephemeralPublicDict)

      let newWire = try ephemeralPublicData.withUnsafeBytes { buffer in
        let rustVec = RustVec<UInt8>(bytes: buffer)
        return try delegate_identity_with_max_age_public(
          parentWire,
          rustVec,
          Constants.temporaryIdentityExpirySecond
        )
      }
      return newWire
    }
  }

  private func fetchAndSetAuthCookie() async throws {
    try await recordThrowingOperation {
      let payload = try createAuthPayload()
      let endpoint = AuthEndpoints.setAnonymousIdentityCookie(payload: payload)

      _ = try await networkService.performRequest(for: endpoint)

      guard let newCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
        throw NetworkError.invalidResponse("Failed to fetch cookie")
      }
      try await extractIdentity(from: newCookie)
    }
  }

  private func extractIdentity(from cookie: HTTPCookie) async throws {
    try await recordThrowingOperation {
      let endpoint = AuthEndpoints.extractIdentity(cookie: cookie)
      let data = try await networkService.performRequest(for: endpoint)
      identityData = data
      if let identityData {
        try KeychainHelper.store(data: identityData, for: keychainIdentityKey)
        try await handleExtractIdentityResponse(from: identityData)
      }
    }
  }

  private func handleExtractIdentityResponse(from data: Data) async throws {
    try await recordThrowingOperation {
      guard !data.isEmpty else {
        throw NetworkError.invalidResponse("Empty identity data received.")
      }
      crashReporter.log("Reached unsafe bytes start")
      let (wire, identity): (DelegatedIdentityWire, DelegatedIdentity) = try data.withUnsafeBytes { buffer in
        guard buffer.count > 0 else {
          throw NetworkError.invalidResponse("Empty data received")
        }
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
        let identity = try delegated_identity_from_bytes(uint8Buffer)
        return (wire, identity)
      }
      crashReporter.log("Reached unsafe bytes end")
      let canistersWrapper = try await authenticate_with_network(wire, nil)
      let principal = canistersWrapper.get_canister_principal()
      let principalString = canistersWrapper.get_canister_principal_string().toString()
      crashReporter.log("canistersWrapper executed succesfully")
      await MainActor.run {
        self.identity = identity
        self.principal = principal
        self.principalString = principalString
      }
    }
  }

  private func createAuthPayload() throws -> Data {
    try recordThrowingOperation {
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

extension DefaultAuthClient {
  @discardableResult
  fileprivate func recordThrowingOperation<T>(_ operation: () throws -> T) throws -> T {
    do {
      return try operation()
    } catch {
      crashReporter.recordException(error)
      throw error
    }
  }

  @discardableResult
  fileprivate func recordThrowingOperation<T>(_ operation: () async throws -> T) async throws -> T {
    do {
      return try await operation()
    } catch {
      crashReporter.recordException(error)
      throw error
    }
  }
}

extension DefaultAuthClient {
  enum Constants {
    static let keychainIdentity = "yral.delegatedIdentity"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
  }
}
