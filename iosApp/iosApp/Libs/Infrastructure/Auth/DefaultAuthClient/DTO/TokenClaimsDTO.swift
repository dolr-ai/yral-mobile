//
//  TokenClaimsDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct TokenClaims: Decodable {
  let aud: String
  let exp: TimeInterval
  let iat: TimeInterval
  let iss: String
  let sub: String
  let nonce: String?
  let extIsAnonymous: Bool
  let delegatedIdentity: SwiftDelegatedIdentityWire

  private enum CodingKeys: String, CodingKey {
    case aud
    case exp
    case iat
    case iss
    case sub
    case nonce
    case extIsAnonymous = "ext_is_anonymous"
    case delegatedIdentity = "ext_delegated_identity"
  }
}
