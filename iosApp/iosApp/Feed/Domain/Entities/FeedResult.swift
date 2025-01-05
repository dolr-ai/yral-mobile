//
//  FeedResult.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

struct FeedResult: Hashable {
  let id: String
  let url: URL
  let thumbnail: URL
  let postDescription: String
  var profileImageURL: URL?
  let likeCount: Int
  let isLiked: Bool
}
