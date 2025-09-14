//
//  VideoEventRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct VideoEventRequest {
  let displayName: String
  let hashtagCount: Int32
  let isHotOrNot: Bool
  let isLoggedIn: Bool
  let isNsfw: Bool
  let likeCount: Int32
  let nsfwProbability: Double
  var percentageWatched: Double = .zero
  let postID: String
  let publisherCanisterID: String
  let publisherUserID: String
  var videoDuration: Double = .zero
  var absoluteWatched: Double = .zero
  let videoID: String
  let viewCount: Int64
  let event: String
}

enum VideoEventType: String {
  case like = "LikeVideo"
  case durationWatched = "VideoDurationWatched"
}
