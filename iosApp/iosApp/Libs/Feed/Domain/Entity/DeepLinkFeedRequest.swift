//
//  DeepLinkFeedRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct DeepLinkFeedRequest {
  let postID: String
  let canisterID: String
  let numViewsAll: Int64?
  let numViewsLoggedIn: Int64?
}
