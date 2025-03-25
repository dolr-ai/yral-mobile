//
//  MoreFeedsRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct MoreFeedsRequest {
  let filteredPosts: [MlFeed_PostItem]
  let numResults: Int
  let feedType: FeedType
}
