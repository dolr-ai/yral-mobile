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

final class DefaultAuthClient: NSObject, AuthClient {
  private(set) var identity: DelegatedIdentity?
  private(set) var canisterPrincipal: Principal?
  private(set) var canisterPrincipalString: String?
  private(set) var userPrincipal: Principal?
  private(set) var userPrincipalString: String?
  private(set) var identityData: Data?

  private let networkService: NetworkService
  private let cookieStorage = HTTPCookieStorage.shared

  private let keychainIdentityKey = Constants.keychainIdentity
  private let keychainPayloadKey = Constants.keychainPayload
  private var pendingAuthState: String!

  private let crashReporter: CrashReporter
  let baseURL: URL

  private let stateSubject = CurrentValueSubject<AuthState, Never>(.uninitialized)
  var authStatePublisher: AnyPublisher<AuthState, Never> {
    stateSubject.eraseToAnyPublisher()
  }

  init(networkService: NetworkService, crashReporter: CrashReporter, baseURL: URL) {
    self.networkService = networkService
    self.crashReporter = crashReporter
    self.baseURL = baseURL
  }

  @MainActor
  func initialize() async throws {
    try await recordThrowingOperation {
      guard let existingCookie = cookieStorage.cookies?.first(where: { $0.name == AuthConstants.cookieName }) else {
        try? KeychainHelper.deleteItem(for: keychainPayloadKey)
        try? KeychainHelper.deleteItem(for: keychainIdentityKey)
        try? KeychainHelper.deleteItem(for: FeedsViewModel.Constants.blockedPrincipalsIdentifier)
        try? KeychainHelper.deleteItem(for: HomeTabController.Constants.eulaAccepted)
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
        self.stateSubject.value = .ephemeralAuthentication(
          userPrincipal: userPrincipalString,
          canisterPrincipal: canisterPrincipalString
        )
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
      self.stateSubject.value = .error(AuthError.authenticationFailed(error.localizedDescription))
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
      self.stateSubject.value = .error(AuthError.authenticationFailed(error.localizedDescription))
      crashReporter.log(error.localizedDescription)
      crashReporter.recordException(error)
      throw error
    }
  }
}

extension DefaultAuthClient: ASWebAuthenticationPresentationContextProviding {
  @MainActor
  func signInWithSocial(provider: SocialProvider) async throws {
    let verifier = PKCE.generateCodeVerifier()
    let challenge = PKCE.codeChallenge(for: verifier)
    let redirect = Constants.redirectURI
    pendingAuthState = UUID().uuidString
    guard let identityData = identityData else { throw AuthError.invalidRequest("No identity data found") }
    let loginHint: String = try identityData.withUnsafeBytes { buffer in
      guard buffer.count > 0 else { throw NetworkError.invalidResponse("Empty data received.") }
      let uint8Buffer = buffer.bindMemory(to: UInt8.self)
      let hint = try yral_auth_login_hint(uint8Buffer)
      return hint.toString()
    }
    let authURL = try getAuthURL(provider: provider, redirect: redirect, challenge: challenge, loginHint: loginHint)
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
          let callbackURL = callbackURL,
          let comps = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false),
          let returned = comps.queryItems?.first(where: { $0.name == "state" })?.value,
          returned == self.pendingAuthState
        else {
          self.stateSubject.value = .error(.authenticationFailed("Invalid OAuth state"))
          return
        }
        guard let code = comps.queryItems?.first(where: { $0.name == "code" })?.value else {
          self.stateSubject.value = .error(.authenticationFailed("Missing authorization code"))
          return
        }
        do {
          try await self.exchangeCodeForTokens(code: code, verifier: verifier, redirectURI: redirect)
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
    loginHint: String? = nil
  ) throws -> URL {
    var comps = URLComponents(string: baseURL.appendingPathComponent(Constants.authPath).absoluteString)!
    comps.queryItems = [
      URLQueryItem(name: "provider", value: provider.rawValue),
      URLQueryItem(name: "client_id", value: Constants.clientID),
      URLQueryItem(name: "response_type", value: "code"),
      URLQueryItem(name: "response_mode", value: "query"),
      URLQueryItem(name: "redirect_uri", value: redirect),
      URLQueryItem(name: "scope", value: "openid"),
      URLQueryItem(name: "code_challenge", value: challenge),
      URLQueryItem(name: "code_challenge_method", value: "S256"),
      URLQueryItem(name: "login_hint", value: loginHint),
      URLQueryItem(name: "state", value: pendingAuthState)
    ]
    guard let authURL = comps.url else { throw AuthError.invalidRequest("Cannot form auth URL") }
    return authURL
  }
  private func exchangeCodeForTokens(code: String, verifier: String, redirectURI: String) async throws {
    let request = TokenRequest(
      grantType: "authorization_code",
      code: code,
      redirectUri: redirectURI,
      clientId: Constants.clientID,
      codeVerifier: verifier
    )
    let formData = try request.formURLEncoded()

    let endpoint = Endpoint(
      http: "socialToken",
      baseURL: baseURL,
      path: Constants.tokenPath,
      method: .post,
      queryItems: nil,
      headers: ["Content-Type": "application/x-www-form-urlencoded"],
      body: formData
    )
    let respData = try await networkService.performRequest(for: endpoint)
    let tokenResponse = try JSONDecoder().decode(TokenResponse.self, from: respData)
    if let jsonString = String(data: respData, encoding: .utf8) {
      print("Token response:\n\(jsonString)")
    }
  }

  func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
    let scenes = UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .filter { $0.activationState == .foregroundActive }
    for scene in scenes {
      if let window = scene.windows.first(where: { $0.isKeyWindow }) {
        return window
      }
    }
    return UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first ?? UIWindow()
  }
}

extension DefaultAuthClient {
  enum Constants {
    static let clientID = "e1a6a7fb-8a1d-42dc-87b4-13ff94ecbe34"
    static let redirectURI = "com.yral.iosApp.staging://oauth/callback"
    static let authPath = "/oauth/auth"
    static let tokenPath = "/oauth/token"
    static let keychainIdentity = "yral.delegatedIdentity"
    static let keychainPayload  = "yral.delegatedIdentityPayload"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
  }
}
