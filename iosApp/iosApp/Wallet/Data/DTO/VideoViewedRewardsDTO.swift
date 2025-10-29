//
//  VideoViewedRewardsDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 02/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct VideoViewedRewardsDTO: Decodable, Equatable {
  let config: VideoViewedRewardsConfigDTO?

  enum CodingKeys: String, CodingKey {
    case config
  }
}

struct VideoViewedRewardsConfigDTO: Decodable, Equatable {
  let rewardAmountINR: Double?
  let rewardAmountUSD: Double?
  let viewMilestone: Int
  let minWatchDuration: Double

  enum CodingKeys: String, CodingKey {
    case rewardAmountINR = "reward_amount_inr"
    case rewardAmountUSD = "reward_amount_usd"
    case viewMilestone = "view_milestone"
    case minWatchDuration = "min_watch_duration"
  }
}
