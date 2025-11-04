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

struct AIPostsResponse: Codable {
  let userID: String
  let count: Int
  let timestamp: Double
  let videos: [AIPostDTO]

  enum CodingKeys: String, CodingKey {
    case userID = "user_id"
    case count, timestamp, videos
  }
}

struct AIPostDTO: Codable {
  let videoID: String
  let postID: String
  let canisterID: String
  let publisherUserID: String
  let numViewsAll: Int64?
  let numViewsLoggedIn: Int64?

  enum CodingKeys: String, CodingKey {
    case postID = "post_id"
    case canisterID = "canister_id"
    case videoID = "video_id"
    case publisherUserID = "publisher_user_id"
    case numViewsAll = "num_views_all"
    case numViewsLoggedIn = "num_views_loggedin"
  }
}

extension AIPostDTO: FeedMapping {
  var nsfwProbability: Double {
    return .zero
  }
}

struct CacheDTO: Codable {
  let postID: String
  let canisterID: String
  let videoID: String
  let nsfwProbability: Double
  let publisherUserID: String
  let numViewsAll: Int64?
  let numViewsLoggedIn: Int64?

  enum CodingKeys: String, CodingKey {
    case postID = "post_id"
    case canisterID = "canister_id"
    case videoID = "video_id"
    case nsfwProbability = "nsfw_probability"
    case publisherUserID = "publisher_user_id"
    case numViewsAll = "num_views_all"
    case numViewsLoggedIn = "num_views_loggedin"
  }
}

extension CacheDTO: FeedMapping { }
