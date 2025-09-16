//
//  VideoEvent.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct VideoEventRequestDTO: Codable {
  let delegatedIdentityWire: SwiftDelegatedIdentityWire
  let events: [VideoEventDTO]

  enum CodingKeys: String, CodingKey {
    case delegatedIdentityWire = "delegated_identity_wire"
    case events
  }
}

struct VideoEventDTO: Codable {
  let event: String
  let canisterId: String
  let creatorCategory: String = ""
  let displayName: String
  let feedType: String = "Clean"
  let hashtagCount: Int32
  let isHotOrNot: Bool
  let isLoggedIn: Bool
  let isNsfw: Bool
  let likeCount: Int32
  let postID: String
  let publisherCanisterId: String
  let publisherUserId: String
  let shareCount: Int = .zero
  let userID: String
  let videoCategory: String = ""
  let videoID: String
  let viewCount: Int32

  let nsfwProbability: Double?
  let absoluteWatched: Double
  let percentageWatched: Double
  let videoDuration: Double

  enum CodingKeys: String, CodingKey {
    case event
    case canisterId = "canister_id"
    case creatorCategory = "creator_category"
    case displayName = "display_name"
    case feedType = "feed_type"
    case hashtagCount = "hashtag_count"
    case isHotOrNot = "is_hot_or_not"
    case isLoggedIn = "is_logged_in"
    case isNsfw = "is_nsfw"
    case likeCount = "like_count"
    case postID = "post_id"
    case publisherCanisterId = "publisher_canister_id"
    case publisherUserId = "publisher_user_id"
    case shareCount = "share_count"
    case userID = "user_id"
    case videoCategory = "video_category"
    case videoID = "video_id"
    case viewCount = "view_count"

    case nsfwProbability = "nsfw_probability"
    case absoluteWatched = "absolute_watched"
    case percentageWatched = "percentage_watched"
    case videoDuration = "video_duration"
  }

  init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)

    self.event = try container.decode(String.self, forKey: .event)
    self.canisterId = try container.decode(String.self, forKey: .canisterId)
    self.displayName = try container.decode(String.self, forKey: .displayName)
    self.hashtagCount = try container.decode(Int32.self, forKey: .hashtagCount)
    self.isHotOrNot = try container.decode(Bool.self, forKey: .isHotOrNot)
    self.isLoggedIn = try container.decode(Bool.self, forKey: .isLoggedIn)
    self.isNsfw = try container.decode(Bool.self, forKey: .isNsfw)
    self.likeCount = try container.decode(Int32.self, forKey: .likeCount)
    self.postID = try container.decode(String.self, forKey: .postID)
    self.publisherCanisterId = try container.decode(String.self, forKey: .publisherCanisterId)
    self.publisherUserId = try container.decode(String.self, forKey: .publisherUserId)
    self.userID = try container.decode(String.self, forKey: .userID)
    self.videoID = try container.decode(String.self, forKey: .videoID)
    self.viewCount = try container.decode(Int32.self, forKey: .viewCount)

    self.nsfwProbability = try container.decodeIfPresent(Double.self, forKey: .nsfwProbability)
    self.absoluteWatched = (try container.decodeIfPresent(Double.self, forKey: .absoluteWatched)) ?? 0
    self.percentageWatched = (try container.decodeIfPresent(Double.self, forKey: .percentageWatched)) ?? 0
    self.videoDuration = (try container.decodeIfPresent(Double.self, forKey: .videoDuration)) ?? 0
  }

  func encode(to encoder: Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)

    try container.encode(event, forKey: .event)
    try container.encode(canisterId, forKey: .canisterId)
    try container.encode(creatorCategory, forKey: .creatorCategory)
    try container.encode(displayName, forKey: .displayName)
    try container.encode(feedType, forKey: .feedType)
    try container.encode(hashtagCount, forKey: .hashtagCount)
    try container.encode(isHotOrNot, forKey: .isHotOrNot)
    try container.encode(isLoggedIn, forKey: .isLoggedIn)
    try container.encode(isNsfw, forKey: .isNsfw)
    try container.encode(likeCount, forKey: .likeCount)
    try container.encode(postID, forKey: .postID)
    try container.encode(publisherCanisterId, forKey: .publisherCanisterId)
    try container.encode(publisherUserId, forKey: .publisherUserId)
    try container.encode(shareCount, forKey: .shareCount)
    try container.encode(userID, forKey: .userID)
    try container.encode(videoCategory, forKey: .videoCategory)
    try container.encode(videoID, forKey: .videoID)
    try container.encode(viewCount, forKey: .viewCount)
    try container.encode(nsfwProbability ?? .zero, forKey: .nsfwProbability)

    if event == VideoEventType.durationWatched.rawValue {
      try container.encode(absoluteWatched, forKey: .absoluteWatched)
      try container.encode(percentageWatched, forKey: .percentageWatched)
      try container.encode(videoDuration, forKey: .videoDuration)
    }
  }

  init(
    event: String,
    canisterId: String,
    displayName: String,
    hashtagCount: Int32,
    isHotOrNot: Bool,
    isLoggedIn: Bool,
    isNsfw: Bool,
    likeCount: Int32,
    postID: String,
    publisherCanisterId: String,
    publisherUserId: String,
    userID: String,
    videoID: String,
    viewCount: Int32,
    nsfwProbability: Double? = nil,
    absoluteWatched: Double = 0,
    percentageWatched: Double = 0,
    videoDuration: Double = 0
  ) {
    self.event = event
    self.canisterId = canisterId
    self.displayName = displayName
    self.hashtagCount = hashtagCount
    self.isHotOrNot = isHotOrNot
    self.isLoggedIn = isLoggedIn
    self.isNsfw = isNsfw
    self.likeCount = likeCount
    self.postID = postID
    self.publisherCanisterId = publisherCanisterId
    self.publisherUserId = publisherUserId
    self.userID = userID
    self.videoID = videoID
    self.viewCount = viewCount
    self.nsfwProbability = nsfwProbability
    self.absoluteWatched = absoluteWatched
    self.percentageWatched = percentageWatched
    self.videoDuration = videoDuration
  }
}

extension VideoEventDTO {
  // swiftlint: disable function_parameter_count
  static func fromDomain(
    _ domain: VideoEventRequest,
    canisterID: String,
    userID: String,
    creatorCategory: String = "",
    feedType: String = "Clean",
    hashtagCount: Int32,
    isHotOrNot: Bool,
    isUserLoggedIn: Bool,
    shareCount: Int = 0,
    videoCategory: String = ""
  ) -> VideoEventDTO {
    return VideoEventDTO(
      event: domain.event,
      canisterId: canisterID,
      displayName: domain.displayName,
      hashtagCount: hashtagCount,
      isHotOrNot: isHotOrNot,
      isLoggedIn: isUserLoggedIn,
      isNsfw: domain.isNsfw,
      likeCount: domain.likeCount,
      postID: domain.postID,
      publisherCanisterId: domain.publisherCanisterID,
      publisherUserId: domain.publisherUserID,
      userID: userID,
      videoID: domain.videoID,
      viewCount: Int32(domain.viewCount),
      nsfwProbability: domain.nsfwProbability,
      absoluteWatched: domain.absoluteWatched,
      percentageWatched: domain.percentageWatched,
      videoDuration: domain.videoDuration
    )
  }
  // swiftlint: enable function_parameter_count
}
