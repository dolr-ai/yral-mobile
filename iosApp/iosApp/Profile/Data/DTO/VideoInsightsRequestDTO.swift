//
//  VideoInsightsRequestDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct VideoInsightsRequestDTO: Encodable {
  let videoIDs: [String]

  enum CodingKeys: String, CodingKey {
    case videoIDs = "video_ids"
  }
}
