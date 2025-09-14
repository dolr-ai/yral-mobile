//
//  CacheDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
struct PostsResponse: Codable {
  let posts: [CacheDTO]
  let processingTime: Double
  let error: String?

  enum CodingKeys: String, CodingKey {
    case posts
    case processingTime = "processing_time_ms"
    case error
  }
}

struct CacheDTO: Codable {
  let postID: String
  let canisterID: String
  let videoID: String
  let nsfwProbability: Double
  let publisherUserID: String

  enum CodingKeys: String, CodingKey {
    case postID = "post_id"
    case canisterID = "canister_id"
    case videoID = "video_id"
    case nsfwProbability = "nsfw_probability"
    case publisherUserID = "publisher_user_id"
  }
}

extension CacheDTO: FeedMapping { }
