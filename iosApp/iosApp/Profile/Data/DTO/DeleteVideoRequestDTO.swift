//
//  DeleteVideoRequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct DeleteVideoRequestDTO: Codable {
  let publisherUserId: String
  let postId: String
  let videoId: String
  let delegatedIdentityWire: SwiftDelegatedIdentityWire

  enum CodingKeys: String, CodingKey {
    case publisherUserId = "publisher_user_id"
    case postId = "post_id"
    case videoId = "video_id"
    case delegatedIdentityWire = "delegated_identity_wire"
  }
}
