//
//  Smiley.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SmileyConfig {
  let smileys: [Smiley]
}

struct Smiley: Identifiable, Hashable {
  let id: String
  let imageName: String
  let isActive: Bool
}

enum SmileyGameResult: Hashable {
  case winner(Smiley, Int)
  case looser(Smiley, Int)
}

struct SmileyGame: Hashable {
  var smileys: [Smiley]
  var result: SmileyGameResult?
}
