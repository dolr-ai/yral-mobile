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
  case otherUsers
}

struct InitialFeedRequest {
  let numResults: Int
  let feedType: FeedType

  init(numResults: Int, feedType: FeedType = .otherUsers) {
    self.numResults = numResults
    self.feedType = feedType
  }
}
