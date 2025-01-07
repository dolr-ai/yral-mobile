//
//  FeedQuery.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct MoreFeedsRequest {
  let filteredPosts: [MlFeed_PostItem]
  let numResults: Int
}

struct InitialFeedRequest {
  let numResults: Int
}
