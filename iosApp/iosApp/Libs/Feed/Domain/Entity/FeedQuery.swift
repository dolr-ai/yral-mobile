//
//  FeedQuery.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

enum FeedType {
  case currentUser
  case otherUser
}

struct MoreFeedsRequest {
  let filteredPosts: [MlFeed_PostItem]
  let numResults: Int
  let feedType: FeedType
}

struct InitialFeedRequest {
  let numResults: Int
  let feedType: FeedType

  init(numResults: Int, feedType: FeedType = .currentUser) {
    self.numResults = numResults
    self.feedType = feedType
  }
}
