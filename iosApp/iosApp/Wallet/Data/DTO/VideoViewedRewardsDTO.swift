//
//  VideoViewedRewardsDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 02/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct VideoViewedRewardsDTO: Decodable {
  let config: VideoViewedRewardsConfigDTO?

  enum CodingKeys: String, CodingKey {
    case config
  }
}

struct VideoViewedRewardsConfigDTO: Decodable {
  let rewardAmountINR: Int
  let viewMilestone: Int
  let minWatchDuration: Int
  let fraudThreshold: Int
  let shodowBanDuration: Int
  let configVersion: Int

  enum CodingKeys: String, CodingKey {
    case rewardAmountINR = "reward_amount_inr"
    case viewMilestone = "view_milestone"
    case minWatchDuration = "min_watch_duration"
    case fraudThreshold = "fraud_threshold"
    case shodowBanDuration = "shadow_ban_duration"
    case configVersion = "config_version"
  }
}
