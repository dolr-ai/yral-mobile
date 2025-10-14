//
//  LeaderboardDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

struct LeaderboardRowDTO: Decodable {
  let principalID: String
  var position: Int
  let wins: Int
  let reward: Int?
  let username: String?

  enum CodingKeys: String, CodingKey {
    case principalID = "principal_id"
    case position, wins, reward, username
  }
}

struct LeaderboardDTO: Decodable {
  let userRow: LeaderboardRowDTO?
  let topRows: [LeaderboardRowDTO]
  let timeLeftInMs: Int?
  let date: String?
  let rewardCurrency: String?
  let rewardCurrencyCode: String?
  let rewardsEnabled: Bool
  let rewardsTable: [String: Int]?

  enum CodingKeys: String, CodingKey {
    case userRow = "user_row"
    case topRows = "top_rows"
    case timeLeftInMs = "time_left_ms"
    case date
    case rewardCurrency = "reward_currency"
    case rewardCurrencyCode = "reward_currency_code"
    case rewardsEnabled = "rewards_enabled"
    case rewardsTable = "rewards_table"
  }
}

extension LeaderboardRowDTO {
  func toDomain() -> LeaderboardRowResponse {
    return LeaderboardRowResponse(
      position: position,
      principalID: principalID,
      wins: wins,
      reward: reward,
      username: username
    )
  }
}

extension LeaderboardDTO {
  func toDomain() -> LeaderboardResponse {
    return LeaderboardResponse(
      userRow: userRow?.toDomain(),
      topRows: topRows.map { $0.toDomain() },
      timeLeftInMs: timeLeftInMs,
      date: date,
      rewardCurrency: rewardCurrency,
      rewardCurrencyCode: rewardCurrencyCode,
      rewardsEnabled: rewardsEnabled,
      rewardsTable: rewardsTable
    )
  }
}
