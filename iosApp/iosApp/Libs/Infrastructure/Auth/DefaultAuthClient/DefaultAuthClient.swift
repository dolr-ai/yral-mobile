//
//  DefaultAuthClient.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Combine
import secp256k1
import AuthenticationServices

// swiftlint: disable type_body_length
final class DefaultAuthClient: NSObject, AuthClient {
  private(set) var identity: DelegatedIdentity?
  private(set) var canisterPrincipal: Principal?
  private(set) var canisterPrincipalString: String?
  private(set) var userPrincipal: Principal?
  private(set) var userPrincipalString: String?
  private(set) var identityData: Data?
  var stateSubject = CurrentValueSubject<AuthState, Never>(.uninitialized)
  var authStatePublisher: AnyPublisher<AuthState, Never> {
    stateSubject.eraseToAnyPublisher()
  }

  let networkService: NetworkService
  let firebaseService: FirebaseService
  let crashReporter: CrashReporter
  let baseURL: URL
  let firebaseBaseURL: URL

  var pendingAuthState: String!

  init(networkService: NetworkService,
       firebaseService: FirebaseService,
       crashReporter: CrashReporter,
       baseURL: URL,
       firebaseBaseURL: URL) {
    self.networkService = networkService
    self.firebaseService = firebaseService
    self.crashReporter = crashReporter
    self.baseURL = baseURL
    self.firebaseBaseURL = firebaseBaseURL
    super.init()
  }

  @MainActor
  func initialize() async throws {
    stateSubject.value = .authenticating
//    try KeychainHelper.deleteItem(for: Constants.keychainIdentity)
//    try KeychainHelper.deleteItem(for: Constants.keychainRefreshToken)
//    try KeychainHelper.deleteItem(for: Constants.keychainAccessToken)
//    try KeychainHelper.deleteItem(for: Constants.keychainIDToken)
//    UserDefaultsManager.shared.remove(.authRefreshTokenExpiryDateKey)
//    UserDefaultsManager.shared.remove(.authIdentityExpiryDateKey)
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

  private func ensureTokensFresh(permanent: Bool) async throws {
    let now = Date().timeIntervalSince1970
    let accessExpiry: Double = UserDefaultsManager.shared
      .get(for: .authIdentityExpiryDateKey)   as Double? ?? .zero
    let refreshExpiry: Double = UserDefaultsManager.shared
      .get(for: .authRefreshTokenExpiryDateKey) as Double? ?? .zero

    if permanent {
      if now < accessExpiry {
        if let data = identityData {
          try await handleExtractIdentityResponse(from: data, type: .permanent)
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
      try await handleExtractIdentityResponse(from: data, type: .ephemeral)
      return
    }

    if now >= refreshExpiry {
      try await obtainAnonymousIdentity()
    } else {
      try await refreshAccessToken()
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
    let refreshToken = try KeychainHelper.retrieveString(for: Constants.keychainRefreshToken) ?? ""
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

  func processDelegatedIdentity(from token: TokenResponse, type: DelegateIdentityType) async throws {
    let claims = try decodeTokenClaims(from: token.idToken)
    let wire = claims.delegatedIdentity
    let wireData = try JSONEncoder().encode(wire)
    identityData = wireData
    try KeychainHelper.store(data: wireData, for: Constants.keychainIdentity)
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
      }

      try await exchangePrincipalID(type: type)
    }
  }

  func getUserBalance(type: DelegateIdentityType) async throws {
    guard let principalID = userPrincipalString else {
      throw SatsCoinError.unknown("Failed to fetch princiapl ID")
    }

    do {
      let response = try await networkService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: URL(string: "https://yral-hot-or-not.go-bazzinga.workers.dev/")!,
          path: "v2/balance/\(principalID)",
          method: .get
        ),
        decodeAs: SatsCoinDTO.self
      ).toDomain()
      await updateAuthState(for: type, withCoins: UInt64(response.balance) ?? 0)
      try await firebaseService.update(coins: UInt64(response.balance) ?? 0, forPrincipal: principalID)
    } catch {
      switch error {
      case let error as NetworkError:
        throw SatsCoinError.network(error)
      default:
        throw SatsCoinError.unknown(error.localizedDescription)
      }
    }
  }

  private func exchangePrincipalID(type: DelegateIdentityType) async throws {
    let newSignIn = try? await firebaseService.signInAnonymously()

    var httpHeaders = [
      "Content-Type": "application/json"
    ]
    let userIDToken = try? await firebaseService.fetchUserIDToken()
    guard let userIDToken else {
      return
    }
    httpHeaders["Authorization"] = "Bearer \(userIDToken)"

    let httpBody: [String: String] = [
      "principal_id": userPrincipalString ?? ""
    ]

    if userPrincipalString != nil, !(newSignIn ?? true) {
      do {
        try await getUserBalance(type: type)
      } catch {
        await updateAuthState(for: type, withCoins: 0)
      }
    } else {
      let endpoint = Endpoint(http: "",
                              baseURL: firebaseBaseURL,
                              path: "exchange_principal_id",
                              method: .post,
                              headers: httpHeaders,
                              body: try? JSONSerialization.data(withJSONObject: httpBody)
                             )

      do {
        let response = try await networkService.performRequest(
          for: endpoint,
          decodeAs: ExchangePrincipalDTO.self
        ).toDomain()
        try await firebaseService.signIn(withCustomToken: response.token)
        try await getUserBalance(type: type)
      } catch {
        await updateAuthState(for: type, withCoins: 0)
      }
    }
  }

  private func updateAuthState(for type: DelegateIdentityType, withCoins coins: UInt64) async {
    await MainActor.run {
      stateSubject.value = (type == .ephemeral) ? .ephemeralAuthentication(
        userPrincipal: userPrincipalString ?? "",
        canisterPrincipal: canisterPrincipalString ?? "",
        coins: coins
      ) : .permanentAuthentication(
        userPrincipal: userPrincipalString ?? "",
        canisterPrincipal: canisterPrincipalString ?? "",
        coins: coins
      )
    }
  }

  @MainActor func logout() async throws {
    try? firebaseService.signOut()
    try? KeychainHelper.deleteItem(for: Constants.keychainIdentity)
    try? KeychainHelper.deleteItem(for: Constants.keychainAccessToken)
    try? KeychainHelper.deleteItem(for: Constants.keychainIDToken)
    try? KeychainHelper.deleteItem(for: Constants.keychainRefreshToken)
    UserDefaultsManager.shared.remove(.authIdentityExpiryDateKey)
    UserDefaultsManager.shared.remove(.authRefreshTokenExpiryDateKey)
    identity = nil
    canisterPrincipal = nil
    canisterPrincipalString = nil
    userPrincipal = nil
    userPrincipalString = nil
    identityData = nil
    UserDefaultsManager.shared.set(false, for: DefaultsKey.userDefaultsLoggedIn)
    stateSubject.value = .loggedOut
    try await obtainAnonymousIdentity()
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
}
// swiftlint: enable type_body_length

enum DelegateIdentityType {
  case ephemeral
  case permanent
}
