//
//  MoreFeedsRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct MoreFeedsRequest {
  let filteredPosts: [FilteredPosts]
  let numResults: Int
  let feedType: FeedType
}

struct FilteredPosts {
  let postID: String
  let canisterID: String
  let videoID: String
  let nsfwProbability: Double
}
