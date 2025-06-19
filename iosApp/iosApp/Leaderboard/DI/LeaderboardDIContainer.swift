//
//  LeaderboardDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class LeaderboardDIContainer {
  struct Dependencies {
    let firebaseService: FirebaseService
    let crashReporter: CrashReporter
    let authClient: AuthClient
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeLeaderboardRepositry() -> LeaderboardRepository {
    LeaderboardRepository(
      firebaseService: dependencies.firebaseService,
      authClient: dependencies.authClient
    )
  }

  func makeLeaderboardViewModel() -> LeaderboardViewModel {
    let leaderboardRepository = makeLeaderboardRepositry()
    return LeaderboardViewModel(
      leaderboardUseCase: LeaderboardUseCase(
        respository: leaderboardRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeLeaderboardView() -> LeaderboardView {
    LeaderboardView(viewModel: makeLeaderboardViewModel())
  }
}
