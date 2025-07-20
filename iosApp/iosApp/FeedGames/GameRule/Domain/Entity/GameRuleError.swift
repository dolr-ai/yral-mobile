//
//  GameRuleError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum GameRuleError: Error {
  case firebaseError(Error)
  case unknown(String)
}

extension GameRuleError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .firebaseError(let firebaseError):
      return "Firebase Error: \(firebaseError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
