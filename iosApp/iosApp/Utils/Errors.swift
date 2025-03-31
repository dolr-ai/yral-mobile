//
//  Utilities.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum RustError: Error {
  case unknown(String)
}

extension RustError: LocalizedError {
  var errorDescription: String? {
    switch self {
    case .unknown(let message):
      return "Rust error: \(message)"
    }
  }
}

struct AggregatedError: Error {
  let errors: [Error]
}

extension AggregatedError: LocalizedError {
  var errorDescription: String? {
    return errors.map {
      ($0 as? LocalizedError)?.errorDescription ?? "\($0)"
    }.joined(separator: "\n")
  }
}
