//
//  ProfileError.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

enum AccountError: Error {
  case invalidInfo(Error)
  case authError(String)
  case rustError(RustError)
  case invalidVideoRequest(String)
  case pageEndReached
  case unkown(String)
}
