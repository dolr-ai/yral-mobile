//
//  LeaderboardUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LeaderboardUseCaseProtocol {
  func execute() async -> Result<LeaderboardResponse, LeaderboardError>
}

class LeaderboardUseCase: BaseResultUseCase<Void, LeaderboardResponse, LeaderboardError>, LeaderboardUseCaseProtocol {

  private let respository: LeaderboardRepositoryProtocol

  init(respository: LeaderboardRepositoryProtocol, crashReporter: CrashReporter) {
    self.respository = respository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<LeaderboardResponse, LeaderboardError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<LeaderboardResponse, LeaderboardError> {
    await respository.fetchLeaderboard()
  }
}
