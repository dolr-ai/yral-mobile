//
//  LeaderboardHistoryUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LeaderboardHistoryUseCaseProtocol {
  func execute() async -> Result<[LeaderboardResponse], LeaderboardError>
}

class LeaderboardHistoryUseCase:
  BaseResultUseCase<Void, [LeaderboardResponse], LeaderboardError>,
  LeaderboardHistoryUseCaseProtocol {

  private let repository: LeaderboardHistoryRepositoryProtocol

  init(
    repository: LeaderboardHistoryRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.repository = repository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<[LeaderboardResponse], LeaderboardError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<[LeaderboardResponse], LeaderboardError> {
    await repository.fetchLeaderboardHistory()
  }
}
