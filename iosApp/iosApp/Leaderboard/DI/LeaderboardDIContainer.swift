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
    let httpService: HTTPService
    let crashReporter: CrashReporter
    let authClient: AuthClient
    let session: SessionManager
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeLeaderboardRepositry() -> LeaderboardRepository {
    LeaderboardRepository(
      firebaseService: dependencies.firebaseService,
      httpService: dependencies.httpService,
      authClient: dependencies.authClient
    )
  }

  @MainActor func makeLeaderboardViewModel() -> LeaderboardViewModel {
    let leaderboardRepository = makeLeaderboardRepositry()
    return LeaderboardViewModel(
      leaderboardUseCase: LeaderboardUseCase(
        respository: leaderboardRepository,
        crashReporter: dependencies.crashReporter
      ), session: dependencies.session
    )
  }

  @MainActor func makeLeaderboardView() -> LeaderboardView {
    LeaderboardView(viewModel: self.makeLeaderboardViewModel())
  }
}
