//
//  LeaderboardHistoryViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum LeaderboardHistoryViewState {
  case initialized
  case loading
  case success
  case failure(Error)

  static func == (lhs: LeaderboardHistoryViewState, rhs: LeaderboardHistoryViewState) -> Bool {
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

class LeaderboardHistoryViewModel: ObservableObject {
  let leaderboardHistoryUseCase: LeaderboardHistoryUseCaseProtocol

  @Published var state: LeaderboardHistoryViewState = .initialized

  var leaderboardHistory: [LeaderboardResponse] = []

  init(leaderboardHistoryUseCase: LeaderboardHistoryUseCaseProtocol) {
    self.leaderboardHistoryUseCase = leaderboardHistoryUseCase
  }

  @MainActor
  func fetchLeaderboardHistory() async {
    state = .loading

    do {
      let result = await leaderboardHistoryUseCase.execute()
      switch result {
      case .success(let response):
        self.leaderboardHistory = response
        state = .success
      case .failure(let error):
        state = .failure(error)
      }
    }
  }
}
