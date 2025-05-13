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
  case ephemeralAuthentication(userPrincipal: String, canisterPrincipal: String)
  case permanentAuthentication(userPrincipal: String, canisterPrincipal: String)
  case loggedOut
  case accountDeleted
  case error(AuthError)

  var isLoggedIn: Bool {
    if case .permanentAuthentication = self { return true }
    return false
  }
}
