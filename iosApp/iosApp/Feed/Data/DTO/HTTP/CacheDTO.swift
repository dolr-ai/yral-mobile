//
//  CacheDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct CacheDTO: Codable {
  let postID: UInt32
  let canisterID: String
  let videoID: String
  let creatorPrincipalID: String

  enum CodingKeys: String, CodingKey {
    case postID = "post_id"
    case canisterID = "canister_id"
    case videoID = "video_id"
    case creatorPrincipalID = "creator_principal_id"
  }
}
