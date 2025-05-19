//
//  SmileyUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol SmileyUseCaseProtocol {
  func execute() async -> Result<SmileyConfig, SmileyConfigError>
}

class SmileyUseCase: BaseResultUseCase<Void, SmileyConfig, SmileyConfigError>, SmileyUseCaseProtocol {

  private let repository: SmileyRepositoryProtocol

  init(repository: SmileyRepositoryProtocol, crashReporter: CrashReporter) {
    self.repository = repository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<SmileyConfig, SmileyConfigError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<SmileyConfig, SmileyConfigError> {
    await repository.fetchSmileyConfig()
  }
}
