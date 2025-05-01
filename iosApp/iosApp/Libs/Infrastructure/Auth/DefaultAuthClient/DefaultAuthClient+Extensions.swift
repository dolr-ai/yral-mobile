//
//  DefaultAuthClient+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
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
}

extension DefaultAuthClient {
  enum Constants {
    static let clientID = "e1a6a7fb-8a1d-42dc-87b4-13ff94ecbe34"
    static let redirectURI = "com.yral.iosApp.staging://oauth/callback"
    static let authPath = "/oauth/auth"
    static let tokenPath = "/oauth/token"
    static let keychainIdentity = "yral.delegatedIdentity"
    static let keychainAccessToken = "yral.accessToken"
    static let keychainRefreshToken = "yral.refreshToken"
    static let keychainTokenExpiryDateKey = "keychainTokenExpiryDate"
    static let temporaryIdentityExpirySecond: UInt64 = 3600
  }
}
