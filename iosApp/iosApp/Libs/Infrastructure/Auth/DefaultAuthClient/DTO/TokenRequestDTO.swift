//
//  RequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 29/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct TokenRequest: Codable {
  let grantType: String
  let code: String
  let redirectUri: String
  let clientId: String
  let codeVerifier: String

  enum CodingKeys: String, CodingKey {
    case grantType = "grant_type"
    case code
    case redirectUri = "redirect_uri"
    case clientId = "client_id"
    case codeVerifier = "code_verifier"
  }
}

extension TokenRequest {
  func formURLEncoded() throws -> Data {
    var comps = URLComponents()
    comps.queryItems = [
      URLQueryItem(name: "grant_type", value: grantType),
      URLQueryItem(name: "code", value: code),
      URLQueryItem(name: "redirect_uri", value: redirectUri),
      URLQueryItem(name: "client_id", value: clientId),
      URLQueryItem(name: "code_verifier", value: codeVerifier)
    ]
    guard let query = comps.percentEncodedQuery,
          let data = query.data(using: .utf8) else {
      throw AuthError.invalidRequest("Unable to form URL-encoded body")
    }
    return data
  }
}
