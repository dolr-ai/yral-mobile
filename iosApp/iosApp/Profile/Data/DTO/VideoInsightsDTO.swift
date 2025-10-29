//
//  VideoInsightsDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct VideoInsightsDTO: Decodable {
  let videoID: String
  let totalViews: UInt64
  let engagedViews: UInt64

  enum CodingKeys: String, CodingKey {
    case videoID = "video_id"
    case totalViews = "total_count_all"
    case engagedViews = "total_count_loggedin"
  }
}
