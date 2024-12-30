//
//  FeedResult.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

struct FeedResult: Hashable {
  let postID: String
  let videoID: String
  let canisterID: String
  let url: URL
  let thumbnail: URL
}
