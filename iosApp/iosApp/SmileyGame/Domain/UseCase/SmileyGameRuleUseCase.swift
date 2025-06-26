//
//  SmileyGameRuleUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import FirebaseFirestore

protocol SmileyGameRuleUseCaseProtocol {
  func execute() async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError>
}

class SmileyGameRuleUseCase: BaseResultUseCase<Void, [SmileyGameRuleResponse], SmileyGameRuleError>,
                             SmileyGameRuleUseCaseProtocol {

  private let repository: SmileyGameRuleRepositoryProtocol

  init(repository: SmileyGameRuleRepositoryProtocol, crashRepoerter: CrashReporter) {
    self.repository = repository
    super.init(crashReporter: crashRepoerter)
  }

  func execute() async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError> {
    await repository.fetchSmileyGameRules()
  }
}
