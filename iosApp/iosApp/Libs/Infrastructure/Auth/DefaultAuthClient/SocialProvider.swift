//
//  SocialProvider.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import iosSharedUmbrella

enum SocialProvider: String {
  case google, apple

  func authJourney() -> AuthJourney {
    switch self {
    case .google:
      return .google
    case .apple:
      return .apple
    }
  }
}
