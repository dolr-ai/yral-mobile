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

  enum CodingKeys: String, CodingKey {
    case principalID = "principal_id"
    case position, wins
  }
}

struct LeaderboardDTO: Decodable {
  let userRow: LeaderboardRowDTO?
  let topRows: [LeaderboardRowDTO]
  let timeLeftInMs: Int?

  enum CodingKeys: String, CodingKey {
    case userRow = "user_row"
    case topRows = "top_rows"
    case timeLeftInMs = "time_left_ms"
  }
}

extension LeaderboardRowDTO {
  func toDomain() -> LeaderboardRowResponse {
    return LeaderboardRowResponse(
      position: position,
      principalID: principalID,
      wins: wins
    )
  }
}

extension LeaderboardDTO {
  func toDomain() -> LeaderboardResponse {
    return LeaderboardResponse(
      userRow: userRow?.toDomain(),
      topRows: topRows.map { $0.toDomain() },
      timeLeftInMs: timeLeftInMs
    )
  }
}
