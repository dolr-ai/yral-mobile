//
//  DefaultAuthClient.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Combine
import P256K
import AuthenticationServices
import iosSharedUmbrella

// swiftlint: disable type_body_length
final class DefaultAuthClient: NSObject, AuthClient {
  private(set) var identity: DelegatedIdentity?
  private(set) var canisterPrincipal: Principal?
  private(set) var canisterPrincipalString: String?
  private(set) var userPrincipal: Principal?
  private(set) var userPrincipalString: String?
  private(set) var identityData: Data?
  var isNewUser = false
  var provider: SocialProvider?
  var stateSubject = CurrentValueSubject<AuthState, Never>(.uninitialized)
  var authStatePublisher: AnyPublisher<AuthState, Never> {
    stateSubject.eraseToAnyPublisher()
  }

  let networkService: NetworkService
  let firebaseService: FirebaseService
  let crashReporter: CrashReporter
  let baseURL: URL
  let satsBaseURL: URL
  let firebaseBaseURL: URL
  let notificationService: NotificationService

  var pendingAuthState: String!

  init(networkService: NetworkService,
       firebaseService: FirebaseService,
       crashReporter: CrashReporter,
       baseURL: URL,
       satsBaseURL: URL,
       firebaseBaseURL: URL,
       notificationService: NotificationService
  ) {
    self.networkService = networkService
    self.firebaseService = firebaseService
    self.crashReporter = crashReporter
    self.baseURL = baseURL
    self.satsBaseURL = satsBaseURL
    self.firebaseBaseURL = firebaseBaseURL
    self.notificationService = notificationService
    super.init()
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(notificationTokenUpdated(_:)),
      name: .registrationTokenUpdated,
      object: nil
    )
  }

  @MainActor
  func initialize() async throws {
    stateSubject.value = .authenticating
    if loadIdentityFromKeychain() {
      if UserDefaultsManager.shared.get(for: DefaultsKey.userDefaultsLoggedIn) as Bool? ?? false {
        try await ensureTokensFresh(permanent: true)
      } else {
        try await ensureTokensFresh(permanent: false)
      }
    } else {
      try await obtainAnonymousIdentity()
    }
  }

  private func loadIdentityFromKeychain() -> Bool {
    guard let data = try? KeychainHelper.retrieveData(for: Constants.keychainIdentity),
          !data.isEmpty else { return false }
    identityData = data
    return true
  }

  fileprivate func setupSession(_ data: Data, type: DelegateIdentityType) async throws {
    try await recordThrowingOperation {
      provider = nil
      if let canisterPrincipalString = try KeychainHelper.retrieveString(
        for: Constants.keychainCanisterPrincipal
      ),
         let userPrincipalString = try KeychainHelper.retrieveString(
          for: Constants.keychainUserPrincipal
         ) {
        let identity: DelegatedIdentity = try data.withUnsafeBytes { buffer in
          guard buffer.count > 0 else {
            throw NetworkError.invalidResponse("Empty data received.")
          }
          let uint8Buffer = buffer.bindMemory(to: UInt8.self)
          let identity = try delegated_identity_from_bytes(uint8Buffer)
          return identity
        }
        self.identity = identity
        self.canisterPrincipalString = canisterPrincipalString
        self.userPrincipalString = userPrincipalString
        self.canisterPrincipal = try get_principal(canisterPrincipalString.intoRustString())
        self.userPrincipal = try get_principal(userPrincipalString.intoRustString())
        Task { @MainActor in
          try await exchangePrincipalID(type: type)
        }
      } else {
        try await handleExtractIdentityResponse(from: data, type: .permanent)
      }
      do {
        try await recordThrowingOperation {
          try await registerForNotifications()
        }
      } catch {
        print(error)
      }
    }
  }

  private func ensureTokensFresh(permanent: Bool) async throws {
    let now = Date().timeIntervalSince1970
    let accessExpiry: Double = UserDefaultsManager.shared
      .get(for: .authIdentityExpiryDateKey)   as Double? ?? .zero
    let refreshExpiry: Double = UserDefaultsManager.shared
      .get(for: .authRefreshTokenExpiryDateKey) as Double? ?? .zero

    if permanent {
      if now < accessExpiry {
        if let data = identityData {
          try await setupSession(data, type: .permanent)
        }
        Task { [weak self] in
          guard let self = self else { return }
          try? await self.refreshAccessToken()
        }
        return
      }

      if now >= refreshExpiry {
        stateSubject.value = .loggedOut
        try await obtainAnonymousIdentity()
        return
      }

      try await refreshAccessToken()
      return
    }

    if now < accessExpiry {
      guard let data = identityData else { return }
      try await setupSession(data, type: .ephemeral)
      Task { @MainActor in
        try await self.handleExtractIdentityResponse(from: data, type: .ephemeral)
      }
      return
    } else {
      try await obtainAnonymousIdentity()
    }
  }

  private func obtainAnonymousIdentity() async throws {
    let requestBody = ClientCredentialsRequest(clientId: Constants.clientID)
    let body = try requestBody.formURLEncodedData()
    let endpoint = Endpoint(
      http: "clientCredentials",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: body
    )
    let resp = try await networkService.performRequest(for: endpoint)
    let token = try JSONDecoder().decode(TokenResponse.self, from: resp)
    try storeTokens(token)
    try await processDelegatedIdentity(from: token, type: .ephemeral)
  }

  private func refreshAccessToken() async throws {
    guard let refreshToken = try KeychainHelper.retrieveString(for: Constants.keychainRefreshToken) else { return }
    let request = RefreshTokenRequest(refreshToken: refreshToken, clientId: Constants.clientID)
    let body = try request.formURLEncodedData()
    let endpoint = Endpoint(
      http: "refreshToken",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: body
    )
    let resp = try await networkService.performRequest(for: endpoint)
    let token = try JSONDecoder().decode(TokenResponse.self, from: resp)
    try storeTokens(token)
    try await processDelegatedIdentity(
      from: token,
      type: (
        UserDefaultsManager.shared.get(for: DefaultsKey.userDefaultsLoggedIn) as Bool? ?? false
      ) ? .permanent : .ephemeral
    )
  }

  func storeTokens(_ token: TokenResponse) throws {
    if let accessTokenData = token.accessToken.data(using: .utf8) {
      try KeychainHelper.store(data: accessTokenData, for: Constants.keychainAccessToken)
    }
    if let iDTokenData = token.idToken.data(using: .utf8) {
      try KeychainHelper.store(data: iDTokenData, for: Constants.keychainIDToken)
    }
    if let refreshTokenData = token.refreshToken.data(using: .utf8) {
      try KeychainHelper.store(data: refreshTokenData, for: Constants.keychainRefreshToken)
    }
    let expiresAt = Date().timeIntervalSince1970 + Double(token.expiresIn)
    UserDefaultsManager.shared.set(expiresAt, for: .authIdentityExpiryDateKey)

    let refreshClaims = try decodeTokenClaims(from: token.refreshToken)
    UserDefaultsManager.shared.set(refreshClaims.exp, for: .authRefreshTokenExpiryDateKey)
  }

  func storeSessionData(identity: Data, canisterPrincipal: String, userPrincipal: String) {
    do {
      try recordThrowingOperation {
        try KeychainHelper.store(data: identity, for: Constants.keychainIdentity)
        try KeychainHelper.store(userPrincipal, for: Constants.keychainUserPrincipal)
        try KeychainHelper.store(canisterPrincipal, for: Constants.keychainCanisterPrincipal)
      }
    } catch {
      print(error)
    }
  }

  func processDelegatedIdentity(from token: TokenResponse, type: DelegateIdentityType) async throws {
    let claims = try decodeTokenClaims(from: token.idToken)
    let wire = claims.delegatedIdentity
    let wireData = try JSONEncoder().encode(wire)
    identityData = wireData
    try await handleExtractIdentityResponse(from: wireData, type: type)
  }

  private func handleExtractIdentityResponse(from data: Data, type: DelegateIdentityType) async throws {
    try await recordThrowingOperation {
      guard !data.isEmpty else {
        throw NetworkError.invalidResponse("Empty identity data received.")
      }
      let (wire, identity): (DelegatedIdentityWire, DelegatedIdentity) = try data.withUnsafeBytes { buffer in
        guard buffer.count > 0 else {
          throw NetworkError.invalidResponse("Empty data received.")
        }
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
        let identity = try delegated_identity_from_bytes(uint8Buffer)
        return (wire, identity)
      }

      let principal = get_principal_from_identity(identity).toString()
      crashReporter.log("Principal id before authenticate_with_network: \(principal)")
      let canistersWrapper = try await authenticate_with_network(wire, nil)

      let canisterPrincipal = canistersWrapper.get_canister_principal()
      let canisterPrincipalString = canistersWrapper.get_canister_principal_string().toString()
      let userPrincipal = canistersWrapper.get_user_principal()
      let userPrincipalString = canistersWrapper.get_user_principal_string().toString()
      crashReporter.setUserId(userPrincipalString)
      await MainActor.run {
        self.identity = identity
        self.canisterPrincipal = canisterPrincipal
        self.canisterPrincipalString = canisterPrincipalString
        self.userPrincipal = userPrincipal
        self.userPrincipalString = userPrincipalString
        self.storeSessionData(
          identity: data,
          canisterPrincipal: canisterPrincipalString,
          userPrincipal: userPrincipalString
        )
      }
      await updateAuthState(for: type, withCoins: .zero, isFetchingCoins: true)
      Task { @MainActor in
        try await exchangePrincipalID(type: type)
      }
    }
  }

  @MainActor func logout() async throws {
    try? firebaseService.signOut()
    try? KeychainHelper.deleteItem(for: Constants.keychainIdentity)
    try? KeychainHelper.deleteItem(for: Constants.keychainUserPrincipal)
    try? KeychainHelper.deleteItem(for: Constants.keychainCanisterPrincipal)
    try? KeychainHelper.deleteItem(for: Constants.keychainAccessToken)
    try? KeychainHelper.deleteItem(for: Constants.keychainIDToken)
    try? KeychainHelper.deleteItem(for: Constants.keychainRefreshToken)
    UserDefaultsManager.shared.remove(.authIdentityExpiryDateKey)
    UserDefaultsManager.shared.remove(.authRefreshTokenExpiryDateKey)
    do {
      try await recordThrowingOperation {
        try await deregisterForNotifications()
      }
    } catch {
      print(error)
    }
    identity = nil
    canisterPrincipal = nil
    canisterPrincipalString = nil
    userPrincipal = nil
    userPrincipalString = nil
    identityData = nil
    UserDefaultsManager.shared.set(false, for: DefaultsKey.userDefaultsLoggedIn)
    AnalyticsModuleKt.getAnalyticsManager().reset()
    stateSubject.value = .loggedOut
    provider = nil
    try await obtainAnonymousIdentity()
    do {
      try await recordThrowingOperation {
        try await registerForNotifications()
      }
    } catch {
      print(error)
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
      let privateKey = try P256K.Signing.PrivateKey(format: .uncompressed)
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
}
// swiftlint: enable type_body_length
