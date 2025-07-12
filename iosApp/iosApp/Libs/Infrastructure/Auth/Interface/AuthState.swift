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

extension AuthState {
  enum Phase: Equatable {
    case uninitialized, authenticating
    case ephemeral, permanent
    case loggedOut, accountDeleted
    case error
  }

  var phase: Phase {
    switch self {
    case .uninitialized: return .uninitialized
    case .authenticating: return .authenticating
    case .ephemeralAuthentication: return .ephemeral
    case .permanentAuthentication: return .permanent
    case .loggedOut: return .loggedOut
    case .accountDeleted: return .accountDeleted
    case .error: return .error
    }
  }
}
