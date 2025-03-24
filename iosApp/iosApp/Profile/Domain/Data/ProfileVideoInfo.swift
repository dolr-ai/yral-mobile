//
//  ProfileVideoMapper.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct ProfileVideoInfo {
  let id: String
  let thumbnailUrl: URL
  let likeCount: Int
}

extension FeedResult {
  func toProfileVideoInfo() -> ProfileVideoInfo {
    return ProfileVideoInfo(
      id: self.postID,
      thumbnailUrl: self.thumbnail,
      likeCount: self.likeCount
    )
  }
}
