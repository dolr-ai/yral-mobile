//
//  RefreshTokenRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 01/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct RefreshTokenRequest: Encodable {
  let grantType: String = "refresh_token"
  let refreshToken: String
  let clientId: String

  private enum CodingKeys: String, CodingKey {
    case grantType = "grant_type"
    case refreshToken = "refresh_token"
    case clientId = "client_id"
  }
}
