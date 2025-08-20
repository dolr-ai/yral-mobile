//
//  FeedRequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 01/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct FeedRequestDTO: Codable {
  let userID: String
  let isNSFW: Bool = false
  let numResults: Int64
  let excludeWatchedItems: [String] = []
  let excludedReportedItems: [String] = []
  let excludedItems: [String] = []

  enum CodingKeys: String, CodingKey {
    case userID = "user_id"
    case isNSFW = "nsfw_label"
    case numResults = "num_results"
    case excludeWatchedItems = "exclude_watched_items"
    case excludedReportedItems = "exclude_reported_items"
    case excludedItems = "exclude_items"
  }
}
