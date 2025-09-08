//
//  LeaderboardDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

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
        repository: leaderboardRepository,
        crashReporter: dependencies.crashReporter
      ), session: dependencies.session
    )
  }

  @MainActor func makeLeaderboardView() -> UINavigationController {
    let rootView = LeaderboardView(viewModel: self.makeLeaderboardViewModel())
      .environment(\.leaderboardNavController, nil)
    let host = UIHostingController(rootView: rootView)
    let navigationController = UINavigationController(rootViewController: host)
    host.rootView = LeaderboardView(viewModel: self.makeLeaderboardViewModel())
      .environment(\.leaderboardNavController, navigationController)

    navigationController.setNavigationBarHidden(true, animated: false)
    navigationController.view.backgroundColor = .clear
    navigationController.edgesForExtendedLayout = .all
    host.edgesForExtendedLayout = .all
    host.extendedLayoutIncludesOpaqueBars = true

    return navigationController
  }
}
