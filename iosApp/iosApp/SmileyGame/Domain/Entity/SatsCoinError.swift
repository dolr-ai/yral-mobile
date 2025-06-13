//
//  SatsCoinError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 10/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum SatsCoinError: Error {
  case network(NetworkError)
  case unknown(String)
}

extension SatsCoinError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .network(let networkError):
      return "Network Error: \(networkError)"
    case .unknown(let errorMessage):
      return "Unknown Error: \(errorMessage)"
    }
  }
}
