//
//  DailyRankError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 04/11/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum DailyRankError: Error {
  case network(NetworkError)
  case cloudFunctionError(CloudFunctionError)
  case firebaseError(Error)
  case unknown(String)
}

extension DailyRankError: LocalizedError {
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
