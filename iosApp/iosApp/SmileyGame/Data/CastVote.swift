//
//  CastVote.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct CastVoteQuery {
  let videoID: String
  let smileyID: String
}

struct SmileyGameResultDTO: Decodable {
  let outcome: String
  let coins: Int
  let coinDelta: Int
  let smiley: SmileyDTO

  enum CodingKeys: String, CodingKey {
    case outcome, coins, smiley
    case coinDelta = "coin_delta"
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
