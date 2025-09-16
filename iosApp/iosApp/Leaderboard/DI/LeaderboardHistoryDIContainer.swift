//
//  LeaderboardHistoryDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

final class LeaderboardHistoryDIContainer {
  struct Dependencies {
    let firebaseService: FirebaseService
    let httpService: HTTPService
    let crashReporter: CrashReporter
    let authClient: AuthClient
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeLeaderboardHistoryRepository() -> LeaderboardHistoryRepository {
    LeaderboardHistoryRepository(
      firebaseService: dependencies.firebaseService,
      httpService: dependencies.httpService,
      authClient: dependencies.authClient
    )
  }

  func makeLeaderboardHistoryViewModel() -> LeaderboardHistoryViewModel {
    let repository = makeLeaderboardHistoryRepository()

    return LeaderboardHistoryViewModel(
      leaderboardHistoryUseCase: LeaderboardHistoryUseCase(
        repository: repository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeLeaderboardHistoryView(onDismiss: @escaping () -> Void) -> UIHostingController<LeaderboardHistoryView> {
    let host = UIHostingController(
      rootView: LeaderboardHistoryView(
        viewModel: makeLeaderboardHistoryViewModel(),
        onDismiss: onDismiss
      )
    )

    host.extendedLayoutIncludesOpaqueBars = true
    return host
  }
}
