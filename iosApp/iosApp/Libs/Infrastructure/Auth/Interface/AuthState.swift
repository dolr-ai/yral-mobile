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
  case ephemeralAuthentication(userPrincipal: String, canisterPrincipal: String, coins: UInt64, isFetchingCoins: Bool)
  case permanentAuthentication(userPrincipal: String, canisterPrincipal: String, coins: UInt64, isFetchingCoins: Bool)
  case loggedOut
  case accountDeleted
  case error(AuthError)

  var isLoggedIn: Bool {
    if case .permanentAuthentication = self { return true }
    return false
  }

  var coins: UInt64 {
    switch self {
    case .ephemeralAuthentication(_, _, let coins, _), .permanentAuthentication(_, _, let coins, _):
      return coins
    default:
      return 0
    }
  }
}
