//
//  LeaderboardResponse.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct LeaderboardRowResponse: Identifiable {
  let id = UUID().uuidString
  let position: Int
  let principalID: String
  let coins: Int
}

struct LeaderboardResponse {
  let userRow: LeaderboardRowResponse
  let rows: [LeaderboardRowResponse]
}

enum LeaderboardError: Error {
  case firebaseError(Error)
  case unknown(String)
}

extension LeaderboardError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .firebaseError(let firebaseError):
      return "Firebase Error: \(firebaseError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
