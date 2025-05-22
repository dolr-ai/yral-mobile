//
//  DefaultAuthClient+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine
import AuthenticationServices

extension DefaultAuthClient {
  @discardableResult
  func recordThrowingOperation<T>(_ operation: () throws -> T) throws -> T {
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
  func recordThrowingOperation<T>(_ operation: () async throws -> T) async throws -> T {
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

  // swiftlint: disable function_body_length
  @MainActor
  func signInWithSocial(provider: SocialProvider) async throws {
    let oldState = stateSubject.value
    stateSubject.value = .authenticating
    do {
      try await recordThrowingOperation {
        pendingAuthState = UUID().uuidString
        let verifier = PKCE.generateCodeVerifier()
        let challenge = PKCE.codeChallenge(for: verifier)
        let redirect = Constants.redirectURI
        let authURL = try getAuthURL(provider: provider,
                                       redirect: redirect,
                                       challenge: challenge,
                                       loginHint: nil)

        let callbackURL: URL = try await withCheckedThrowingContinuation { continuation in
          let session = ASWebAuthenticationSession(
            url: authURL,
            callbackURLScheme: redirect.components(separatedBy: "://")[0]
          ) { url, error in
            if let error = error {
              continuation.resume(throwing: error)
            } else if let url = url {
              continuation.resume(returning: url)
            } else {
              continuation.resume(throwing: AuthError.authenticationFailed("No callback URL"))
            }
          }
          session.presentationContextProvider = self
          session.prefersEphemeralWebBrowserSession = true
          session.start()
        }

        let comps = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)
        guard
          let state = comps?.queryItems?.first(where: { $0.name == "state" })?.value,
          state == pendingAuthState,
          let code  = comps?.queryItems?.first(where: { $0.name == "code" })?.value
        else {
          throw AuthError.authenticationFailed("Invalid callback parameters")
        }

        let token = try await exchangeCodeForTokens(
          code: code,
          verifier: verifier,
          redirectURI: redirect
        )
        try storeTokens(token)
        try await processDelegatedIdentity(from: token, type: .permanent)
        UserDefaultsManager.shared.set(true, for: .userDefaultsLoggedIn)
        guard let canisterPrincipalString = self.canisterPrincipalString else { return }
        Task {
          do {
            try await updateSession(canisterID: canisterPrincipalString)
          } catch {
            crashReporter.recordException(error)
          }
        }
      }
    } catch {
      stateSubject.value = oldState
      throw error
    }
  }
  // swiftlint: enable function_body_length

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

  func decodeTokenClaims(from jwt: String) throws -> TokenClaimsDTO {
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
    return try JSONDecoder().decode(TokenClaimsDTO.self, from: data)
  }

  func updateSession(canisterID: String) async throws {
    try await recordThrowingOperation {
      guard let accessTokenData = try KeychainHelper.retrieveData(for: Constants.keychainAccessToken),
            let accessTokenString = String(data: accessTokenData, encoding: .utf8),
            let url = URL(string: Constants.yralMetaDataBaseURLString) else { return }
      let body = Data("{}".utf8)
      let endpoint = Endpoint(
        http: "update_session_as_registered",
        baseURL: url,
        path: Constants.sessionRegistrationPath + canisterID,
        method: .post,
        headers: [
          "authorization": "Bearer \(accessTokenString)",
          "Content-Type": "application/json"
        ],
        body: body
      )
      try await networkService.performRequest(for: endpoint)
    }
  }
}

extension DefaultAuthClient {
  enum Constants {
    static let clientID = "e1a6a7fb-8a1d-42dc-87b4-13ff94ecbe34"
    static let redirectURI = "com.yral.iosApp.staging://oauth/callback"
    static let authPath = "/oauth/auth"
    static let tokenPath = "/oauth/token"
    static let keychainIdentity = "yral.delegatedIdentity"
    static let keychainAccessToken = "yral.accessToken"
    static let keychainIDToken = "yral.idToken"
    static let keychainRefreshToken = "yral.refreshToken"
    static let keychainTokenExpiryDateKey = "keychainTokenExpiryDate"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
    static let keychainDeletedVideosKey = "keychainDeletedVideosKey"
    static let yralMetaDataBaseURLString = "https://yral-metadata.fly.dev"
    static let sessionRegistrationPath = "/update_session_as_registered/"
  }
}
