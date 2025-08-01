//
//  FeedError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum FeedError: Error {
  case authError(AuthError)
  case networkError(NetworkError)
  case rustError(RustError)
  case unknown(String)
  case aggregated(AggregatedError, [FeedResult]?)
  case firebaseError(String)
  case rechargeError(String)
}

extension FeedError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .authError(let authError):
      return "Authentication Error: \(authError.localizedDescription)"
    case .networkError(let networkError):
      return "Network Error: \(networkError.localizedDescription)"
    case .rustError(let rustError):
      return "Rust Error: \(rustError.localizedDescription)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    case .aggregated(let aggregated, _):
      return "Aggregated Error: \(aggregated.localizedDescription)"
    case .firebaseError(let message):
      return "Firebase Error: \(message)"
    case .rechargeError(let error):
      return "Tap to recharge error: \(error)"
    }
  }
}
