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
  let crashReporter: CrashReporter
  private let baseURL: URL

  private var pendingAuthState: String!

  init(networkService: NetworkService, crashReporter: CrashReporter, baseURL: URL) {
    self.networkService = networkService
    self.crashReporter = crashReporter
    self.baseURL = baseURL
    super.init()
  }

  @MainActor
  func initialize() async throws {
    stateSubject.value = .authenticating
//    try KeychainHelper.deleteItem(for: Constants.keychainIdentity)
//    try KeychainHelper.deleteItem(for: Constants.keychainRefreshToken)
//    try KeychainHelper.deleteItem(for: Constants.keychainAccessToken)
//    UserDefaultsManager.shared.remove(.keychainTokenExpiryDateKey)
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
    let expiry: Double = UserDefaultsManager.shared.get(for: .keychainTokenExpiryDateKey) as Double? ?? .zero

    if now >= expiry {
      // token really is expired → either re‐login or refresh
      if permanent {
        stateSubject.value = .loggedOut
        try await obtainAnonymousIdentity()
      } else {
        try await refreshAccessToken()
      }
    } else {
      // still fresh
      if permanent {
        try await refreshAccessToken()    // optional: you could skip or refresh early
      } else {
        guard let data = identityData else { return }
        try await handleExtractIdentityResponse(from: data, type: .ephemeral)
      }
    }
  }

  private func obtainAnonymousIdentity() async throws {
    let requestBody = ClientCredentialsRequest(clientId: Constants.clientID)
    let bodyData = try requestBody.formURLEncoded()
    let endpoint = Endpoint(
      http: "clientCredentials",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: bodyData
    )
    let resp = try await networkService.performRequest(for: endpoint)
    let token = try JSONDecoder().decode(TokenResponse.self, from: resp)
    try storeTokens(token)
    try await processDelegatedIdentity(from: token, type: .ephemeral)
  }

  private func refreshAccessToken() async throws {
    let refreshToken = try KeychainHelper.retrieveString(for: Constants.keychainRefreshToken) ?? ""
    let body = "grant_type=refresh_token&refresh_token=\(refreshToken)&client_id=\(Constants.clientID)"
    let data = Data(body.utf8)
    let endpoint = Endpoint(
      http: "refreshToken",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: data
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

  private func storeTokens(_ token: TokenResponse) throws {
    if let accessTokenData = token.accessToken.data(using: .utf8) {
      try KeychainHelper.store(data: accessTokenData, for: Constants.keychainAccessToken)
    }
    if let refreshTokenData = token.refreshToken.data(using: .utf8) {
      try KeychainHelper.store(data: refreshTokenData, for: Constants.keychainRefreshToken)
    }
    let expiresAt = Date().timeIntervalSince1970 + Double(token.expiresIn)
    UserDefaultsManager.shared.set(expiresAt, for: .keychainTokenExpiryDateKey)
  }

  private func processDelegatedIdentity(from token: TokenResponse, type: DelegateIdentityType) async throws {
    let claims = try decodeClaims(from: token.accessToken)
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
        stateSubject.value = (type == .ephemeral) ? .ephemeralAuthentication(
          userPrincipal: userPrincipalString,
          canisterPrincipal: canisterPrincipalString
        ) : .permanentAuthentication(
          userPrincipal: userPrincipalString,
          canisterPrincipal: canisterPrincipalString
        )
      }
    }
  }

  func logout() async throws {
    UserDefaultsManager.shared.set(false, for: DefaultsKey.userDefaultsLoggedIn)
    try? KeychainHelper.deleteItem(for: Constants.keychainIdentity)
    try? KeychainHelper.deleteItem(for: Constants.keychainAccessToken)
    try? KeychainHelper.deleteItem(for: Constants.keychainRefreshToken)
    UserDefaultsManager.shared.remove(.keychainTokenExpiryDateKey)
    identity = nil
    canisterPrincipal = nil
    canisterPrincipalString = nil
    userPrincipal = nil
    userPrincipalString = nil
    identityData = nil
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

  @MainActor
  func signInWithSocial(provider: SocialProvider) async throws {
    pendingAuthState = UUID().uuidString
    let verifier = PKCE.generateCodeVerifier()
    let challenge = PKCE.codeChallenge(for: verifier)
    let redirect = Constants.redirectURI
    let authURL = try getAuthURL(
      provider: provider,
      redirect: redirect,
      challenge: challenge,
      loginHint: nil
    )
    let session = ASWebAuthenticationSession(
      url: authURL,
      callbackURLScheme: redirect.components(separatedBy: "://")[0]
    ) { callbackURL, error in
      Task {
        if let error = error {
          self.stateSubject.value = .error(.authenticationFailed(error.localizedDescription))
          return
        }
        guard
          let url = callbackURL,
          let comps = URLComponents(url: url, resolvingAgainstBaseURL: false),
          let state = comps.queryItems?.first(where: { $0.name == "state" })?.value,
          state == self.pendingAuthState,
          let code = comps.queryItems?.first(where: { $0.name == "code" })?.value
        else {
          self.stateSubject.value = .error(.authenticationFailed("Invalid callback"))
          return
        }
        do {
          let token = try await self.exchangeCodeForTokens(
            code: code,
            verifier: verifier,
            redirectURI: redirect
          )
          try self.storeTokens(token)
          try await self.processDelegatedIdentity(from: token, type: .permanent)
          UserDefaultsManager.shared.set(true, for: .userDefaultsLoggedIn)
        } catch {
          self.stateSubject.value = .error(.authenticationFailed(error.localizedDescription))
        }
      }
    }
    session.presentationContextProvider = self
    session.prefersEphemeralWebBrowserSession = true
    session.start()
  }

  private func getAuthURL(
    provider: SocialProvider,
    redirect: String,
    challenge: String,
    loginHint: String?
  ) throws -> URL {
    guard var comps = URLComponents(
      string: baseURL.appendingPathComponent(Constants.authPath).absoluteString
    ) else { throw AuthError.invalidRequest("Invalid url components") }
    comps.queryItems = [
      .init(name: "provider", value: provider.rawValue),
      .init(name: "client_id", value: Constants.clientID),
      .init(name: "response_type", value: "code"),
      .init(name: "response_mode", value: "query"),
      .init(name: "redirect_uri", value: redirect),
      .init(name: "scope", value: "openid"),
      .init(name: "code_challenge", value: challenge),
      .init(name: "code_challenge_method", value: "S256"),
      .init(name: "state", value: pendingAuthState)
    ]
    guard let url = comps.url else { throw AuthError.invalidRequest("Bad URL") }
    return url
  }

  private func exchangeCodeForTokens(
    code: String,
    verifier: String,
    redirectURI: String
  ) async throws -> TokenResponse {
    let req = TokenRequest(
      grantType: "authorization_code",
      code: code,
      redirectUri: redirectURI,
      clientId: Constants.clientID,
      codeVerifier: verifier
    )
    let form = try req.formURLEncoded()
    let endpoint = Endpoint(
      http: "exchangeCode",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: form
    )
    let data = try await networkService.performRequest(for: endpoint)
    return try JSONDecoder().decode(TokenResponse.self, from: data)
  }

  func decodeClaims(from jwt: String) throws -> TokenClaims {
    let parts = jwt.split(separator: ".")
    guard parts.count == 3 else {
      throw AuthError.authenticationFailed("Malformed JWT")
    }
    let rawPayload = String(parts[1])
    let paddedLength = ((rawPayload.count + 3) / 4) * 4
    let base64 = rawPayload
      .padding(toLength: paddedLength, withPad: "=", startingAt: 0)
      .replacingOccurrences(of: "-", with: "+")
      .replacingOccurrences(of: "_", with: "/")
    guard let data = Data(base64Encoded: base64) else {
      throw AuthError.authenticationFailed("JWT payload not Base64")
    }
    return try JSONDecoder().decode(TokenClaims.self, from: data)
  }
}

enum DelegateIdentityType {
  case ephemeral
  case permanent
}

// swiftlint: enable type_body_length
