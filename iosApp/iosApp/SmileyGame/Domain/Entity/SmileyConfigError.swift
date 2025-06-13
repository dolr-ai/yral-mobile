//
//  SmileyError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum SmileyConfigError: Error {
  case firebaseError(Error)
  case unknown(String)
}

extension SmileyConfigError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .firebaseError(let firebaseError):
      return "Firebase Error: \(firebaseError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
