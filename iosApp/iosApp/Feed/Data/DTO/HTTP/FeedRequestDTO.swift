//
//  FeedRequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 01/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct FeedRequestDTO: Codable {
  let canisterID: String
  let filterResults: [FilteredResultDTO]
  let numResults: Int64

  enum CodingKeys: String, CodingKey {
    case canisterID = "canister_id"
    case filterResults = "filter_results"
    case numResults = "num_results"
  }
}

struct FilteredResultDTO: Codable {
  let canisterID: String
  let postID: Int64
  let videoID: String
  let nsfwProbability: Double

  enum CodingKeys: String, CodingKey {
    case canisterID = "canister_id"
    case postID = "post_id"
    case videoID = "video_id"
    case nsfwProbability = "nsfw_probability"
  }
}

extension FilteredPosts {
  func asFilteredResultDTO() -> FilteredResultDTO {
    FilteredResultDTO(
      canisterID: canisterID,
      postID: Int64(postID) ?? .zero,
      videoID: videoID,
      nsfwProbability: self.nsfwProbability
    )
  }
}
