//
//  LeaderboardViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine

enum LeaderboardViewState {
  case initialized
  case loading
  case success
  case failure(Error)

  static func == (lhs: LeaderboardViewState, rhs: LeaderboardViewState) -> Bool {
    switch (lhs, rhs) {
    case (.initialized, .initialized),
      (.loading, .loading),
      (.success, .success):
      return true
    case (.failure, .failure):
      return true
    default:
      return false
    }
  }
}

@MainActor
class LeaderboardViewModel: ObservableObject {
  private let leaderboardUseCase: LeaderboardUseCaseProtocol
  private var cancellables = Set<AnyCancellable>()
  var leaderboardResponse: LeaderboardResponse?

  @Published var state: LeaderboardViewState = .initialized
  private(set) var coinsReady = false

  init(leaderboardUseCase: LeaderboardUseCaseProtocol, session: SessionManager) {
    self.leaderboardUseCase = leaderboardUseCase

    session.coinsReadyPublisher
      .prefix(1)
      .sink { [weak self] _ in
        guard let self else { return }
        Task { @MainActor in
          self.coinsReady = true
          await self.fetchLeaderboard()
        }
      }
      .store(in: &cancellables)
  }

  func fetchLeaderboard() async {
    state = .loading

    let result = await leaderboardUseCase.execute()
    await MainActor.run {
      switch result {
      case .success(let success):
        self.leaderboardResponse = success
        self.state = .success
      case .failure(let failure):
        self.state = .failure(failure)
        print("Failed to fetch leaderboard: \(failure)")
      }
    }
  }

  func refreshLeaderboardIfReady() async {
    guard coinsReady else { return }
    await fetchLeaderboard()
  }

  func fetchImageURL(for principal: String) -> URL? {
    if let principal = try? get_principal(principal) {
      return URL(string: propic_from_principal(principal).toString())
    } else {
      return nil
    }
  }
}
