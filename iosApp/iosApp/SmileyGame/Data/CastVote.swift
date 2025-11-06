//
//  CastVote.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct CastVoteQuery: Encodable {
  let videoID: String
  let smileyID: String
  var principalID: String?

  enum CodingKeys: String, CodingKey {
    case videoID = "video_id"
    case smileyID = "smiley_id"
    case principalID = "principal_id"
  }
}

extension CastVoteQuery {
  func addingPrincipal(_ id: String) -> CastVoteQuery {
    var copy = self
    copy.principalID = id
    return copy
  }
}

struct SmileyGameResultDTO: Decodable {
  let outcome: String
  let coins: UInt64
  let coinDelta: Int
  let newPosition: Int
  let smiley: SmileyDTO

  enum CodingKeys: String, CodingKey {
    case outcome, coins, smiley
    case coinDelta = "coin_delta"
    case newPosition = "new_position"
  }
}

// struct SmileyGameResultErrorDTO: Decodable, Error {
//  let error: SmileyGameErrorDTO
// }
//
// struct SmileyGameErrorDTO: Decodable {
//  let code: String
//  let message: String
// }
