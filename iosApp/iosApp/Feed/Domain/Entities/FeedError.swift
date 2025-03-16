//
//  FeedError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

enum FeedError: Error {
  case authError(AuthError)
  case networkError(NetworkError)
  case rustError(RustError)
  case unknown(String)
}
