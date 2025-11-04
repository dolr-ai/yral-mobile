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
  let coins: UInt64
  let coinDelta: Int
  let newPosition: Int
  let smiley: Smiley
}
