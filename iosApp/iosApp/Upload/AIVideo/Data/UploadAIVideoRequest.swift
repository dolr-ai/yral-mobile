//
//  UploadAIVideoRequest.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct UploadAIVideoRequest: Encodable {
  let videoURL: String
  let hashtags: [String]
  let description: String
  let isNSFW: Bool
  let enableHotOrNot: Bool
  let delegatedIdentityWire: SwiftDelegatedIdentityWire

  enum CodingKeys: String, CodingKey {
    case hashtags, description
    case videoURL = "video_url"
    case isNSFW = "is_nsfw"
    case enableHotOrNot = "enable_hot_or_not"
    case delegatedIdentityWire = "delegated_identity_wire"
  }
}
