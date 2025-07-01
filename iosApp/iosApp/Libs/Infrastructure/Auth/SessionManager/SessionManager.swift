//
//  SessionManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine
import iosSharedUmbrella

@MainActor
final class SessionManager: ObservableObject {
  @Published private(set) var state: AuthState = .uninitialized
  private var bag: AnyCancellable?

  init(auth: AuthClient) {
    bag = auth.authStatePublisher
      .receive(on: RunLoop.main)
      .assign(to: \.state, on: self)
  }

  func update(coins: UInt64) {
    switch state {
    case .ephemeralAuthentication(let userPrincipal, let canisterPrincipal, _, _):
      state = .ephemeralAuthentication(
        userPrincipal: userPrincipal,
        canisterPrincipal: canisterPrincipal,
        coins: coins,
        isFetchingCoins: false
      )
      AnalyticsModuleKt.getAnalyticsManager().setUserProperties(
        user: User(
          userId: userPrincipal,
          canisterId: canisterPrincipal,
          userType: UserType.theNew,
          tokenWalletBalance: Double(coins),
          tokenType: TokenType.sats
        )
      )
    case .permanentAuthentication(let userPrincipal, let canisterPrincipal, _, _):
      state = .permanentAuthentication(
        userPrincipal: userPrincipal,
        canisterPrincipal: canisterPrincipal,
        coins: coins,
        isFetchingCoins: false
      )
      AnalyticsModuleKt.getAnalyticsManager().setUserProperties(
        user: User(
          userId: userPrincipal,
          canisterId: canisterPrincipal,
          userType: UserType.existing,
          tokenWalletBalance: Double(coins),
          tokenType: TokenType.sats
        )
      )
    default:
      break
    }
  }
}

extension SessionManager {
  var coinsReadyPublisher: AnyPublisher<Void, Never> {

    let authPhase = $state
      .map { state -> Bool in
        switch state {
        case .ephemeralAuthentication, .permanentAuthentication:
          return true
        default:
          return false
        }
      }
      .removeDuplicates()

    return authPhase
      .compactMap { $0 ? () : nil }
      .flatMap { [weak self] _ -> AnyPublisher<Void, Never> in
        guard let self else { return Empty().eraseToAnyPublisher() }

        return self.$state
          .compactMap { state -> Void? in
            switch state {
            case .ephemeralAuthentication(_, _, _, let fetching),
                .permanentAuthentication(_, _, _, let fetching):
              return fetching ? nil : ()
            default:
              return nil
            }
          }
          .prefix(.one)
          .eraseToAnyPublisher()
      }
      .eraseToAnyPublisher()
  }
}
