//
//  ResponseDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 29/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct TokenResponse: Codable {
  let accessToken: String
  let idToken: String
  let tokenType: String
  let expiresIn: Int
  let refreshToken: String
  enum CodingKeys: String, CodingKey {
    case accessToken = "access_token"
    case tokenType = "token_type"
    case expiresIn = "expires_in"
    case refreshToken = "refresh_token"
    case idToken = "id_token"
  }
}
