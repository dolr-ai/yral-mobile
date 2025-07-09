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
  var position: Int?
  let coins: Int
}

struct LeaderboardDTO {
  let userRow: LeaderboardRowDTO
  let rows: [LeaderboardRowDTO]
}

extension LeaderboardRowDTO {
  func toDomain() -> LeaderboardRowResponse {
    return LeaderboardRowResponse(
      position: position ?? .zero,
      principalID: id ?? UUID().uuidString,
      coins: coins
    )
  }
}

extension LeaderboardDTO {
  func toDomain() -> LeaderboardResponse {
    return LeaderboardResponse(
      userRow: userRow.toDomain(),
      rows: rows.map { $0.toDomain() }
    )
  }
}
