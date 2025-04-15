//
//  AuthErrors.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum AuthError: Error {
  case authenticationFailed(String)
}

extension AuthError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .authenticationFailed(let message):
      return "Authentication Failed: \(message)"
    }
  }
}
