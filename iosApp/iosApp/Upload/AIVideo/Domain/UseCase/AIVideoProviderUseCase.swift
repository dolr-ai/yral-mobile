//
//  AIVideoProviderUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol AIVideoProviderUseCaseProtocol {
  func execute() async -> Result<AIVideoProviderMetaResponse, AIVideoProviderError>
}

class AIVideoProviderUseCase: BaseResultUseCase<Void, AIVideoProviderMetaResponse, AIVideoProviderError>,
                              AIVideoProviderUseCaseProtocol {
  func execute() async -> Result<AIVideoProviderMetaResponse, AIVideoProviderError> {
    await super.execute(request: ())
  }

  private let aiVideoRepository: AIVideoRepositoryProtocol

  init(aiVideoRepository: AIVideoRepositoryProtocol,
       crashReporter: CrashReporter) {
    self.aiVideoRepository = aiVideoRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: Void) async -> Result<AIVideoProviderMetaResponse, AIVideoProviderError> {
    await aiVideoRepository.getProviders()
  }
}
