//
//  WalletError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum WalletError: Error {
  case authError(AuthError)
  case rustError(RustError)
  case networkError(NetworkError)
  case firebaseError(String)
  case unknown(String)
}

extension WalletError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .authError(let message):
      return "Authentication Error: \(message)"
    case .rustError(let rustError):
      return "Rust Error: \(rustError.localizedDescription)"
    case .networkError(let message):
      return "Network Error: \(message)"
    case .firebaseError(let message):
      return "Firebase Error: \(message)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
