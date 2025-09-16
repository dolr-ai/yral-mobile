//
//  UploadAIVideoError.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum UploadAIVideoError: Error {
  case network(NetworkError)
  case auth(AuthError)
  case unknown(Error)
}

extension UploadAIVideoError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .network(let networkError):
      return "Network Error: \(networkError.localizedDescription)"
    case .auth(let authError):
      return "Authentication Error: \(authError.localizedDescription)"
    case .unknown(let error):
      return "Unknown Error: \(error.localizedDescription)"
    }
  }
}
