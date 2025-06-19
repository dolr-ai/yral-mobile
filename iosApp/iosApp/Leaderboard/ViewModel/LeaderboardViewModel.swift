//
//  LeaderboardViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

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

class LeaderboardViewModel: ObservableObject {
  let leaderboardUseCase: LeaderboardUseCaseProtocol
  var leaderboardResponse: LeaderboardResponse?

  @Published var state: LeaderboardViewState = .initialized

  init(leaderboardUseCase: LeaderboardUseCaseProtocol) {
    self.leaderboardUseCase = leaderboardUseCase
  }

  func fetchLeaderbaord() async {
    await MainActor.run {
      state = .loading
    }

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

  func fetchImageURL(for principal: String) -> URL? {
    if let principal = try? get_principal(principal) {
      return URL(string: propic_from_principal(principal).toString())
    } else {
      return nil
    }
  }
}
