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
  let principalID: String
  let url: URL
  let hashtags: [String]
  let thumbnail: URL
  var viewCount: Int64
  let displayName: String
  let postDescription: String
  var profileImageURL: URL?
  var likeCount: Int
  var isLiked: Bool
  var nsfwProbability: Double
  var smileyGame: SmileyGame?
  private var identity: String { "\(canisterID)#\(postID)" }

  func hash(into hasher: inout Hasher) {
    hasher.combine(identity)
  }

  static func == (lhs: FeedResult, rhs: FeedResult) -> Bool {
    lhs.identity == rhs.identity
  }
}
