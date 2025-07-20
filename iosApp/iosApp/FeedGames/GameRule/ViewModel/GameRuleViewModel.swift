//
//  GameRuleViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum GameRuleViewState {
  case initialized
  case loading
  case success
  case failure(Error)

  static func == (lhs: GameRuleViewState, rhs: GameRuleViewState) -> Bool {
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

class GameRuleViewModel: ObservableObject {
  let gameRuleUseCase: GameRuleUseCaseProtocol
  var gameRuleResponse: [GameRuleResponse]?
  let feedGame: FeedGame

  @Published var state: GameRuleViewState = .initialized

  init(gameRuleUseCase: GameRuleUseCaseProtocol, feedGame: FeedGame) {
    self.gameRuleUseCase = gameRuleUseCase
    self.feedGame = feedGame
  }

  func fetchGameRules() async {
    let result = await gameRuleUseCase.execute(request: feedGame)
    await MainActor.run {
      switch result {
      case .success(let success):
        self.gameRuleResponse = success
        self.state = .success
      case .failure(let failure):
        self.state = .failure(failure)
        print("Failed to fetch smiley game rules: \(failure)")
      }
    }
  }
}
