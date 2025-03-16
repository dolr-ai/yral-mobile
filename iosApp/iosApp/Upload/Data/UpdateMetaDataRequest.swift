//////
//////  UpadteMetaRequest.swift
//////  iosApp
//////
//////  Created by Sarvesh Sharma on 06/03/25.
//////  Copyright © 2025 orgName. All rights reserved.
//////
////
import Foundation

struct UpdateMetaDataRequest: Codable {
  let videoUid: String
  let delegatedIdentityWire: SwiftDelegatedIdentityWire
  let meta: [String: String]
  let postDetails: PostDetailsFromFrontendRequest

  enum CodingKeys: String, CodingKey {
    case videoUid = "video_uid"
    case delegatedIdentityWire = "delegated_identity_wire"
    case meta
    case postDetails = "post_details"
  }
}

struct PostDetailsFromFrontendRequest: Codable {
  let isNsfw: Bool
  let hashtags: [String]
  let description: String
  let videoUid: String
  let creatorConsentForInclusionInHotOrNot: Bool

  enum CodingKeys: String, CodingKey {
    case isNsfw = "is_nsfw"
    case hashtags
    case description
    case videoUid = "video_uid"
    case creatorConsentForInclusionInHotOrNot = "creator_consent_for_inclusion_in_hot_or_not"
  }

  init(fromRust rust: PostDetailsFromFrontendRef) {
    self.isNsfw = rust.is_nsfw()
    self.hashtags = rust.hashtags().map { $0.as_str().toString() }
    self.description = rust.description().toString()
    self.videoUid = rust.video_uid().toString()
    self.creatorConsentForInclusionInHotOrNot = rust.creator_consent_for_inclusion_in_hot_or_not()
  }

  init(
    isNsfw: Bool = false,
    hashtags: [String],
    description: String,
    videoUid: String,
    creatorConsentForInclusionInHotOrNot: Bool = true
  ) {
    self.isNsfw = isNsfw
    self.hashtags = hashtags
    self.description = description
    self.videoUid = videoUid
    self.creatorConsentForInclusionInHotOrNot = creatorConsentForInclusionInHotOrNot
  }
}

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
