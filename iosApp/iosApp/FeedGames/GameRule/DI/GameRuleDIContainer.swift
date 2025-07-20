//
//  GameRuleDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class GameRuleDIContainer {
  struct Dependencies {
    let firebaseService: FirebaseService
    let crashReporter: CrashReporter
    let feedGame: FeedGame
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeGameRuleRepository() -> GameRuleRepository {
    GameRuleRepository(firebaseService: dependencies.firebaseService)
  }

  func makeGameRuleViewModel() -> GameRuleViewModel {
    let gameRuleRepository = makeGameRuleRepository()
    return GameRuleViewModel(
      gameRuleUseCase: GameRuleUseCase(
        repository: gameRuleRepository,
        crashRepoerter: dependencies.crashReporter
      ),
      feedGame: dependencies.feedGame
    )
  }

  func makeGameRuleView(onDismiss: @escaping () -> Void) -> GameRuleView {
    GameRuleView(viewModel: makeGameRuleViewModel(), onDismiss: onDismiss)
  }
}
