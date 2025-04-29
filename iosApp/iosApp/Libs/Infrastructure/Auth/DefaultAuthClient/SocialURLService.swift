//
//  SocialURLService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum SocialURLService {
  static let base = URL(string: "https://yral-auth-v2.fly.dev")!

  case authorize(provider: SocialProvider, codeChallenge: String, redirectURI: String)
  case token(authCode: String, codeVerifier: String, redirectURI: String)

  var urlRequest: URLRequest {
    switch self {
    case .authorize(let provider, let codeChallenge, let redirectURI):
      var comps = URLComponents(url: Self.base.appendingPathComponent("/oauth/auth"), resolvingAgainstBaseURL: false)!
      comps.queryItems = [
        .init(name: "provider", value: provider.rawValue),
        .init(name: "client_id", value: Constants.clientID),
        .init(name: "response_type", value: "code"),
        .init(name: "response_mode", value: "query"),
        .init(name: "redirect_uri", value: redirectURI),
        .init(name: "scope", value: "openid"),
        .init(name: "code_challenge", value: codeChallenge),
        .init(name: "code_challenge_method", value: "S256")
      ]
      return URLRequest(url: comps.url!)

    case .token(let code, let verifier, let redirectURI):
      var request = URLRequest(url: Self.base.appendingPathComponent("/oauth/token"))
      request.httpMethod = "POST"
      let body: [String: String] = [
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": redirectURI,
        "client_id": Constants.clientID,
        "code_verifier": verifier
      ]
      request.httpBody = try? JSONEncoder().encode(body)
      request.setValue("application/json", forHTTPHeaderField: "Content-Type")
      return request
    }
  }

  var endpoint: Endpoint {
    switch self {
    case .authorize(_, _, let redirectURI):
      let comps = URLComponents(url: Self.base.appendingPathComponent("/oauth/auth"),
                                resolvingAgainstBaseURL: false)!
      return Endpoint(
        http: "socialAuthorize",
        baseURL: Self.base,
        path: "/oauth/auth",
        method: .get,
        queryItems: comps.queryItems
      )

    case .token(let code, let verifier, let redirectURI):
      let bodyDict: [String: String] = [
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": redirectURI,
        "client_id": Constants.clientID,
        "code_verifier": verifier
      ]
      let bodyData = try? JSONEncoder().encode(bodyDict)
      return Endpoint(
        http: "socialToken",
        baseURL: Self.base,
        path: "/oauth/token",
        method: .post,
        headers: ["Content-Type": "application/json"],
        body: bodyData
      )
    }
  }
}

enum SocialProvider: String {
  case google, apple
}

extension SocialURLService {
  enum Constants {
    static let clientID = "https://www.facebook.com/"
  }
}
