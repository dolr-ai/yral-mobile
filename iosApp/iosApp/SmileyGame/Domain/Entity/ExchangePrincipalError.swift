//
//  ExchangePrincipalError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum ExchangePrincipalError: Error {
  case network(NetworkError)
  case firebaseError(Error)
  case unknown(String)
}

extension ExchangePrincipalError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .network(let networkError):
      return "Network Error: \(networkError)"
    case .firebaseError(let firebaseError):
      return "Firebase Error: \(firebaseError)"
    case .unknown(let message):
      return "Unknown Error: \(message)"
    }
  }
}
