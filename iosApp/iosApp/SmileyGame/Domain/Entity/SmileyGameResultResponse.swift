//
//  SmileyGameResultResponse.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct SmileyGameResultResponse: Hashable {
  let videoID: String
  let outcome: String
  let coins: Int
  let coinDelta: Int
  let smiley: Smiley
}
