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
  lazy var phasePublisher: AnyPublisher<AuthState.Phase, Never> = {
    let subject = CurrentValueSubject<AuthState.Phase, Never>(self.state.phase)
    return self.$state
      .map(\.phase)
      .removeDuplicates()
      .multicast(subject: subject)
      .autoconnect()
      .eraseToAnyPublisher()
  }()

  private var bag = Set<AnyCancellable>()

  init(auth: AuthClient) {
    auth.authStatePublisher
      .receive(on: RunLoop.main)
      .assign(to: \.state, on: self)
      .store(in: &bag)
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
          isLoggedIn: false,
          isCreator: nil,
          walletBalance: KotlinDouble(value: Double(coins)),
          tokenType: .yral
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
          isLoggedIn: true,
          isCreator: nil,
          walletBalance: KotlinDouble(value: Double(coins)),
          tokenType: .yral
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
