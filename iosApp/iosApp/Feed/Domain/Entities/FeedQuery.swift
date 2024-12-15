//
//  FeedQuery.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct FeedRequest {
  let canisterId: String
  let filteredPosts: [MlFeed_PostItem]
  let numResults: Int
}
