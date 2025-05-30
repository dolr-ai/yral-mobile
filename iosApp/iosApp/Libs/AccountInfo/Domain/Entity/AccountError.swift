//
//  ProfileError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum AccountError: Error {
  case invalidInfo(String)
  case authError(AuthError)
  case rustError(RustError)
  case invalidVideoRequest(String)
  case networkError(String)
  case pageEndReached
  case unknown(String)
  case keychainError(String)
}

extension AccountError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .invalidInfo(let message):
      return "Invalid Info: \(message)"
    case .authError(let message):
      return "Authentication Error: \(message)"
    case .rustError(let rustError):
      return "Rust Error: \(rustError.localizedDescription)"
    case .invalidVideoRequest(let message):
      return "Invalid Video Request: \(message)"
    case .networkError(let message):
      return "Network Error: \(message)"
    case .pageEndReached:
      return "Page end reached."
    case .unknown(let message):
      return "Unknown Error: \(message)"
    case .keychainError(let message):
      return "Unknown Error: \(message)"
    }
  }
}
