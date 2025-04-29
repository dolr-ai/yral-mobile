//
//  SmileyGameRuleError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum SmileyGameRuleError: Error {
  case networkError(NetworkError)
  case unknown(String)
}

extension SmileyGameRuleError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .networkError(let networkError):
      return "Network Error: \(networkError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
