//
//  CacheDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
struct PostsResponse: Codable {
    let posts: [CacheDTO]
}

struct CacheDTO: Codable {
  let postID: UInt32
  let canisterID: String
  let videoID: String
  let nsfwProbability: Double

  enum CodingKeys: String, CodingKey {
    case postID = "post_id"
    case canisterID = "canister_id"
    case videoID = "video_id"
    case nsfwProbability = "nsfw_probability"
  }
}

extension CacheDTO: FeedMapping { }
