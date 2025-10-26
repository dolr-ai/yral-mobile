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
  let wins: Int
  let reward: Int?
  let username: String
}

struct LeaderboardResponse {
  let userRow: LeaderboardRowResponse?
  let topRows: [LeaderboardRowResponse]
  var timeLeftInMs: Int?
  let date: String?
  let rewardCurrency: String?
  let rewardCurrencyCode: String?
  let rewardsEnabled: Bool
  let rewardsTable: [String: Int]?
}

enum LeaderboardError: Error {
  case network(NetworkError)
  case cloudFunctionError(CloudFunctionError)
  case firebaseError(Error)
  case unknown(String)
}

extension LeaderboardError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .network(let networkError):
      return "Network Error: \(networkError)"
    case .cloudFunctionError(let cloudFunctionError):
      return "Cloud Function Error: \(cloudFunctionError)"
    case .firebaseError(let firebaseError):
      return "Firebase Error: \(firebaseError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
