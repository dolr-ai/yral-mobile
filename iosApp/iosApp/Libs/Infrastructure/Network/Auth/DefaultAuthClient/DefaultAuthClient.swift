//
//  DefaultAuthClient.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import secp256k1

class DefaultAuthClient: AuthClient {

  private(set) var identity: DelegatedIdentity?
  private(set) var canisterPrincipal: Principal?
  private(set) var canisterPrincipalString: String?
  private(set) var userPrincipal: Principal?
  private(set) var userPrincipalString: String?

  private let networkService: NetworkService
  private let cookieStorage = HTTPCookieStorage.shared

  private(set) var identityData: Data?

  private let keychainIdentityKey = Constants.keychainIdentity

  private let keychainPayloadKey = Constants.keychainPayload

  private let crashReporter: CrashReporter

  init(networkService: NetworkService, crashReporter: CrashReporter) {
    self.networkService = networkService
    self.crashReporter = crashReporter
  }

  @MainActor
  func initialize() async throws {
    try await recordThrowingOperation {
      guard let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
        try? KeychainHelper.deleteItem(for: keychainPayloadKey)
        try? KeychainHelper.deleteItem(for: keychainIdentityKey)
        try? KeychainHelper.deleteItem(for: FeedsViewModel.Constants.blockedPrincipalsIdentifier)
        try await fetchAndSetAuthCookie()
        return
      }

      try await refreshAuthIfNeeded(using: existingCookie)
    }
  }

  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws {
    try await recordThrowingOperation {
      if let expiresDate = cookie.expiresDate, expiresDate < Date() {
        try await fetchAndSetAuthCookie()
      } else {
        do {
          if let data = try KeychainHelper.retrieveData(for: keychainIdentityKey), !data.isEmpty {
            identityData = data
            try await handleExtractIdentityResponse(from: data)
          } else {
            try await extractIdentity(from: cookie)
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
        throw NetworkError.invalidResponse("No identity data available.")
      }
      return try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
        if buffer.count > 0 {
          let uint8Buffer = buffer.bindMemory(to: UInt8.self)
          return try delegated_identity_from_bytes(uint8Buffer)
        } else {
          throw NetworkError.invalidResponse("Empty identity data received.")
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
        guard buf.count > 0 else {
          throw NetworkError.invalidResponse("Empty parent identity data.")
        }
        return try delegated_identity_wire_from_bytes(buf.bindMemory(to: UInt8.self))
      }

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

      let jwkData = try JSONSerialization.data(withJSONObject: jwk, options: [])

      let newWire = try jwkData.withUnsafeBytes { buffer in
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

  func logout() {
    if let cookies = cookieStorage.cookies {
      for cookie in cookies where cookie.name == AuthConstants.cookieName {
        cookieStorage.deleteCookie(cookie)
      }
    }

    try? KeychainHelper.deleteItem(for: keychainIdentityKey)
    try? KeychainHelper.deleteItem(for: keychainPayloadKey)

    self.identity = nil
    self.canisterPrincipal = nil
    self.canisterPrincipalString = nil
    self.userPrincipal = nil
    self.userPrincipalString = nil
    self.identityData = nil
  }

  private func fetchAndSetAuthCookie() async throws {
    try await recordThrowingOperation {
      let payload = try createOrRetrieveAuthPayload()
      let endpoint = AuthEndpoints.setAnonymousIdentityCookie(payload: payload)
      _ = try await networkService.performRequest(for: endpoint)
      guard let newCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
        throw NetworkError.invalidResponse("Failed to fetch cookie from setAnonymousIdentityCookie response.")
      }
      try await extractIdentity(from: newCookie)
    }
  }

  private func extractIdentity(from cookie: HTTPCookie) async throws {
    try await recordThrowingOperation {
      let endpoint = AuthEndpoints.extractIdentity(cookie: cookie)
      let data = try await networkService.performRequest(for: endpoint)

      identityData = data
      try KeychainHelper.store(data: data, for: keychainIdentityKey)

      try await handleExtractIdentityResponse(from: data)
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
          throw NetworkError.invalidResponse("Empty data received.")
        }
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
        let identity = try delegated_identity_from_bytes(uint8Buffer)
        return (wire, identity)
      }
      crashReporter.log("Reached unsafe bytes end")

      let principal = get_principal_from_identity(identity).toString()
      crashReporter.log("Principal id before authenticate_with_network: \(principal)")
      let canistersWrapper = try await authenticate_with_network(wire, nil)
      crashReporter.log("canistersWrapper authenticate_with_network success")

      let canisterPrincipal = canistersWrapper.get_canister_principal()
      let canisterPrincipalString = canistersWrapper.get_canister_principal_string().toString()
      let userPrincipal = canistersWrapper.get_user_principal()
      let userPrincipalString = canistersWrapper.get_user_principal_string().toString()
      crashReporter.log("canistersWrapper executed successfully")
      crashReporter.setUserId(userPrincipalString)
      await MainActor.run {
        self.identity = identity
        self.canisterPrincipal = canisterPrincipal
        self.canisterPrincipalString = canisterPrincipalString
        self.userPrincipal = userPrincipal
        self.userPrincipalString = userPrincipalString
      }
    }
  }

  private func createOrRetrieveAuthPayload() throws -> Data {
    if let stored = try? KeychainHelper.retrieveData(for: keychainPayloadKey),
        !stored.isEmpty {
      return stored
    }

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
    let payloadData = try JSONSerialization.data(withJSONObject: payload)

    try KeychainHelper.store(data: payloadData, for: keychainPayloadKey)
    return payloadData
  }

  @discardableResult
  fileprivate func recordThrowingOperation<T>(_ operation: () throws -> T) throws -> T {
    do {
      return try operation()
    } catch {
      crashReporter.log(error.localizedDescription)
      crashReporter.recordException(error)
      throw error
    }
  }

  @discardableResult
  fileprivate func recordThrowingOperation<T>(_ operation: () async throws -> T) async throws -> T {
    do {
      return try await operation()
    } catch {
      crashReporter.log(error.localizedDescription)
      crashReporter.recordException(error)
      throw error
    }
  }
}

extension DefaultAuthClient {
  enum Constants {
    static let keychainIdentity = "yral.delegatedIdentity"
    static let keychainPayload  = "yral.delegatedIdentityPayload"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
  }
}
