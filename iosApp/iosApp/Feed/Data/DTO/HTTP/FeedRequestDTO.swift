//
//  FeedRequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 01/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct FeedRequestDTO: Codable {
  let userID: String
  let filterResults: [FilteredResultDTO]
  let numResults: Int64

  enum CodingKeys: String, CodingKey {
    case userID = "user_id"
    case filterResults = "filter_results"
    case numResults = "num_results"
  }
}

struct FilteredResultDTO: Codable {
  let canisterID: String
  let isNsfw: Bool
  let postID: Int64
  let publisherUserID: String
  let videoID: String

  enum CodingKeys: String, CodingKey {
    case canisterID = "canister_id"
    case isNsfw = "is_nsfw"
    case postID = "post_id"
    case publisherUserID = "publisher_user_id"
    case videoID = "video_id"
  }
}

extension FilteredPosts {
  func asFilteredResultDTO() -> FilteredResultDTO {
    FilteredResultDTO(
      canisterID: canisterID,
      isNsfw: self.isNsfw,
      postID: Int64(postID) ?? .zero,
      publisherUserID: self.publisherUserID,
      videoID: videoID
    )
  }
}
