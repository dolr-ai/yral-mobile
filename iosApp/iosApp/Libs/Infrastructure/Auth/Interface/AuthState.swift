//
//  AuthState.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

public enum AuthState: Equatable {
  case uninitialized
  case authenticating
  case ephemeralAuthentication(userPrincipal: String, canisterPrincipal: String, coins: Int)
  case permanentAuthentication(userPrincipal: String, canisterPrincipal: String, coins: Int)
  case loggedOut
  case accountDeleted
  case error(AuthError)

  var isLoggedIn: Bool {
    if case .permanentAuthentication = self { return true }
    return false
  }

  var coins: Int {
    switch self {
    case .ephemeralAuthentication(_, _, let coins), .permanentAuthentication(_, _, let coins):
      return coins
    default:
      return 0
    }
  }
}
