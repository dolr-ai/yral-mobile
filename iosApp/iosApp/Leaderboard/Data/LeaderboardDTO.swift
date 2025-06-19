//
//  LeaderboardDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

struct LeaderboardRowDTO: Decodable {
  @DocumentID var id: String?
  let coins: Int
}

struct LeaderboardDTO {
  let userRow: LeaderboardRowDTO
  let rows: [LeaderboardRowDTO]
}

extension LeaderboardRowDTO {
  func toDomain() -> LeaderboardRowResponse {
    return LeaderboardRowResponse(
      principalID: id ?? UUID().uuidString,
      coins: coins
    )
  }
}

extension LeaderboardDTO {
  func toDomain(userPosition: Int) -> LeaderboardResponse {
    return LeaderboardResponse(
      userPosition: userPosition,
      userRow: userRow.toDomain(),
      rows: rows.map { $0.toDomain() }
    )
  }
}
