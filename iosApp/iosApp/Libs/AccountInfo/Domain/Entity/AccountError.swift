//
//  ProfileError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum AccountError: Error {
  case invalidInfo(String)
  case authError(String)
  case rustError(RustError)
  case invalidVideoRequest(String)
  case networkError(String)
  case pageEndReached
  case unknown(String)
}
