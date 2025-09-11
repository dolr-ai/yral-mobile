//
//  VideoUploadError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum VideoUploadError: Error {
  case invalidFileURL(String)
  case invalidUploadURL(String)
  case network(NetworkError)
  case auth(AuthError)
  case unknown(Error)
}

extension VideoUploadError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .invalidFileURL(let message):
      return "Invalid File URL: \(message)"
    case .invalidUploadURL(let message):
      return "Invalid Upload URL: \(message)"
    case .network(let networkError):
      return "Network Error: \(networkError.localizedDescription)"
    case .auth(let authError):
      return "Authentication Error: \(authError.localizedDescription)"
    case .unknown(let error):
      return "Unknown Error: \(error.localizedDescription)"
    }
  }
}
