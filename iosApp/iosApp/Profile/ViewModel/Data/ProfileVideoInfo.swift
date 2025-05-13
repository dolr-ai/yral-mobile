//
//  ProfileVideoMapper.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct ProfileVideoInfo: Equatable {
  let id = UUID()
  let postID: String
  let videoId: String
  let thumbnailUrl: URL
  let likeCount: Int
  let isLiked: Bool
}

extension FeedResult {
  func toProfileVideoInfo() -> ProfileVideoInfo {
    return ProfileVideoInfo(
      postID: self.postID,
      videoId: self.videoID,
      thumbnailUrl: self.thumbnail,
      likeCount: self.likeCount,
      isLiked: self.isLiked
    )
  }
}
