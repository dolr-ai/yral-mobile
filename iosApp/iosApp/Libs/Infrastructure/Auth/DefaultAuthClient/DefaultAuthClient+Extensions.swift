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
import iosSharedUmbrella
import FirebaseMessaging
import Mixpanel
import MixpanelSessionReplay

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

  // swiftlint: disable function_body_length cyclomatic_complexity
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
        guard let data = identityData else {
          throw NetworkError.invalidResponse("No identity data available.")
        }
        let loginHint = try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
          if buffer.count > 0 {
            let uint8Buffer = buffer.bindMemory(to: UInt8.self)
            return try yral_auth_login_hint(uint8Buffer).toString()
          }
          return ""
        }
        let authURL = try getAuthURL(
          provider: provider,
          redirect: redirect,
          challenge: challenge,
          loginHint: loginHint
        )

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
        try firebaseService.signOut()
        let oldPrincipal = self.userPrincipalString
        do {
          try await deregisterForNotifications()
        } catch {
          crashReporter.log(error.localizedDescription)
          crashReporter.recordException(error)
        }
        try await processDelegatedIdentity(from: token, type: .permanent)
        UserDefaultsManager.shared.set(true, for: .userDefaultsLoggedIn)
        isNewUser = oldPrincipal == self.userPrincipalString
        self.provider = provider
        guard let canisterPrincipalString = self.canisterPrincipalString else { return }
        guard let userPrincipalString = self.userPrincipalString else { return }
        do {
          try await registerForNotifications()
        } catch {
          crashReporter.log(error.localizedDescription)
          crashReporter.recordException(error)
        }
        Task {
          do {
            try await updateSession(canisterID: canisterPrincipalString, principalID: userPrincipalString)
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
  // swiftlint: enable function_body_length cyclomatic_complexity

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
      .init(name: "login_hint", value: loginHint),
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

  func updateSession(canisterID: String, principalID: String) async throws {
    let oldState = stateSubject.value
    do {
      guard let accessTokenData = try KeychainHelper.retrieveData(for: Constants.keychainAccessToken),
            let accessTokenString = String(data: accessTokenData, encoding: .utf8),
            let url = URL(string: Constants.yralMetaDataBaseURLString) else { return }
      let request = UpdateSessionDTO(canisterId: canisterID, userPrincipal: principalID)
      let endpoint = Endpoint(
        http: "update_session_as_registered",
        baseURL: url,
        path: Constants.sessionRegistrationPath,
        method: .post,
        headers: [
          "authorization": "Bearer \(accessTokenString)",
          "Content-Type": "application/json"
        ],
        body: try JSONEncoder().encode(request)
      )
      try await networkService.performRequest(for: endpoint)
    } catch {
      stateSubject.value = oldState
    }
  }

  @objc func notificationTokenUpdated(_ notification: Notification) {
    Task {
      return try await recordThrowingOperation {
        guard let token = notification.object as? String else { return }
        let identity = try self.generateNewDelegatedIdentity()
        try await register_device(identity, token.intoRustString())
      }
    }
  }

  func registerForNotifications() async throws {
    let token = notificationService.getRegistrationToken()
    let identity = try self.generateNewDelegatedIdentity()
    try await register_device(identity, token.intoRustString())
  }

  func deregisterForNotifications() async throws {
    let token = notificationService.getRegistrationToken()
    let identity = try self.generateNewDelegatedIdentity()
    try await unregister_device(identity, token.intoRustString())
  }

  func setAnalyticsData() async {
    do {
      try await recordThrowingOperation {
        try await setAnalyticsUserProperties()
        guard provider != nil else { return }
        if isNewUser {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: SignupSuccessEventData(
              isReferral: false,
              referralUserID: "",
              authJourney: provider!.authJourney(),
              affiliate: AppDIHelper().getAffiliateAttributionStore().peek()
            )
          )
        } else {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: LoginSuccessEventData(
              authJourney: provider!.authJourney(),
              affiliate: AppDIHelper().getAffiliateAttributionStore().peek()
            )
          )
        }
        provider = nil
      }
    } catch {
      print(error)
    }
  }

  // swiftlint: disable large_tuple
  private func setAnalyticsUserProperties() async throws {
    let identity = try self.generateNewDelegatedIdentity()
    let principal = try get_principal(self.canisterPrincipalString ?? "")
    let service = try Service(principal, identity)
    var isCreator = false
    do {
      let result = try await service.get_posts_of_this_user_profile_with_pagination_cursor(
        UInt64(CGFloat.zero),
        UInt64(CGFloat.two)
      )
      if result.is_ok() {
        guard let postResult = result.ok_value() else { return }
        isCreator = postResult.count > .zero
      }
    } catch {
      print(error)
    }
    let (userPrincipal, canisterPrincipal, email, coins, isLoggedIn): (String, String, String?, UInt64, Bool) = {
      switch stateSubject.value {
      case .ephemeralAuthentication(let userPrincipal, let canisterPrincipal, let coins, _, _):
        return (userPrincipal, canisterPrincipal, nil, coins, false)
      case .permanentAuthentication(let userPrincipal, let canisterPrincipal, let email, let coins, _, _):
        return (userPrincipal, canisterPrincipal, email, coins, true)
      default:
        return ("", "", nil, .zero, false)
      }
    }()
    AnalyticsModuleKt.getAnalyticsManager().setUserProperties(
      user: User(
        userId: userPrincipal,
        canisterId: canisterPrincipal,
        isLoggedIn: KotlinBoolean(bool: isLoggedIn),
        isCreator: KotlinBoolean(bool: isCreator),
        walletBalance: KotlinDouble(value: Double(coins)),
        tokenType: .yral,
        isForcedGamePlayUser: KotlinBoolean(
          bool: AppDIHelper().getFeatureFlagManager().isEnabled(
            flag: FeedFeatureFlags.SmileyGame.shared.StopAndVoteNudge
          )
        ),
        emailId: email
      )
    )
    MPSessionReplay.getInstance()?.identify(distinctId: Mixpanel.sharedInstance()?.distinctId ?? "")
  }
  // swiftlint: enable large_tuple
}

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

extension DefaultAuthClient {
  enum Constants {
    static let clientID = "e1a6a7fb-8a1d-42dc-87b4-13ff94ecbe34"
    static var redirectURI: String {
      guard let uri = Bundle.main.object(forInfoDictionaryKey: "YRAL_REDIRECT_URI") as? String,
            !uri.isEmpty
      else {
        fatalError("YRAL_REDIRECT_URI missing from Info.plist")
      }
      return uri
    }
    static let authPath = "/oauth/auth"
    static let tokenPath = "/oauth/token"
    static let keychainIdentity = "yral.delegatedIdentity"
    static let keychainCanisterPrincipal = "yral.canisterPrincipal"
    static let keychainUserPrincipal = "yral.userPrincipal"
    static let keychainEmail = "yral.userEmail"
    static let keychainAccessToken = "yral.accessToken"
    static let keychainIDToken = "yral.idToken"
    static let keychainRefreshToken = "yral.refreshToken"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
    static let yralMetaDataBaseURLString = "https://yral-metadata.fly.dev"
    static let sessionRegistrationPath = "/v2/update_session_as_registered"
  }
}

enum DelegateIdentityType {
  case ephemeral
  case permanent
}
