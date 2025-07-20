//
//  GameRuleUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import FirebaseFirestore

protocol GameRuleUseCaseProtocol {
  func execute(request: FeedGame) async -> Result<[GameRuleResponse], GameRuleError>
}

class GameRuleUseCase: BaseResultUseCase<FeedGame, [GameRuleResponse], GameRuleError>,
                             GameRuleUseCaseProtocol {

  private let repository: GameRuleRepositoryProtocol

  init(repository: GameRuleRepositoryProtocol, crashRepoerter: CrashReporter) {
    self.repository = repository
    super.init(crashReporter: crashRepoerter)
  }

  override func runImplementation(_ request: FeedGame) async -> Result<[GameRuleResponse], GameRuleError> {
    await repository.fetchRules(for: request)
  }
}
