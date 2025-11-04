//
//  DailyRankResponse.swift
//  iosApp
//
//  Created by Samarth Paboowal on 04/11/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct DailyRankResponse: Decodable {
  let principalID: String
  let wins: Int
  var position: Int

  enum CodingKeys: String, CodingKey {
    case wins, position
    case principalID = "principal_id"
  }
}
