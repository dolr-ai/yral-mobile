//
//  SwiftDelegatedIdentityWire.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct SwiftDelegatedIdentityWire: Codable {
  let fromKey: [UInt8]
  let toSecret: SwiftJwkEcKey
  let delegationChain: [SwiftSignedDelegation]

  enum CodingKeys: String, CodingKey {
    case fromKey = "from_key"
    case toSecret = "to_secret"
    case delegationChain = "delegation_chain"
  }
}

// swiftlint: disable identifier_name
struct SwiftJwkEcKey: Codable {
  let kty: String
  let crv: String
  let x: String
  let y: String
  let d: String
}
// swiftlint: enable identifier_name

struct SwiftSignedDelegation: Codable {
  let delegation: SwiftDelegation
  let signature: [UInt8]
}

struct SwiftDelegation: Codable {
  let pubkey: [UInt8]
  let expiration: UInt64
  let targets: [String]?
}
