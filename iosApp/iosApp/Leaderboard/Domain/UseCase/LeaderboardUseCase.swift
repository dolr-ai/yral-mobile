//
//  LeaderboardUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LeaderboardUseCaseProtocol {
  func execute(request: LeaderboardQuery) async -> Result<LeaderboardResponse, LeaderboardError>
}

class LeaderboardUseCase: BaseResultUseCase
<LeaderboardQuery, LeaderboardResponse, LeaderboardError>,
LeaderboardUseCaseProtocol {

  private let repository: LeaderboardRepositoryProtocol

  init(repository: LeaderboardRepositoryProtocol, crashReporter: CrashReporter) {
    self.repository = repository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: LeaderboardQuery) async ->
  Result<LeaderboardResponse, LeaderboardError> {
    await repository.fetchLeaderboard(request: request)
  }
}
