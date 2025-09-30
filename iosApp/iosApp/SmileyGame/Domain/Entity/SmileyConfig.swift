//
//  Smiley.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SmileyConfig: Hashable {
  var smileys: [Smiley]
  let lossPenalty: Int
}

struct Smiley: Identifiable, Hashable {
  let id: String
  let imageURL: String
  let isActive: Bool
  let clickAnimation: String
  let fallbackImage: String
}

enum SmileyGameState: Hashable {
  case notPlayed
  case voted
  case played(SmileyGameResultResponse)
  case error(String)
}

struct SmileyGame: Hashable {
  var config: SmileyConfig
  var state: SmileyGameState
}
