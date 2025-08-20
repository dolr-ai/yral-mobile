//
//  MoreFeedsRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct MoreFeedsRequest {
  let numResults: Int
  let feedType: FeedType
}

struct FilteredPosts {
  let canisterID: String
  let isNsfw: Bool
  let postID: String
  let publisherUserID: String
  let videoID: String
}
