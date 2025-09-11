//
//  GenerateVideoStatusUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol GenerateVideoStatusUseCaseProtocol {
  func execute(request: UInt64) async -> Result<String, GenerateVideoStatusError>
}

class GenerateVideoStatusUseCase: BaseResultUseCase<
UInt64, String, GenerateVideoStatusError>,
GenerateVideoStatusUseCaseProtocol {
  private let aiVideoRepository: AIVideoRepositoryProtocol

  init(
    aiVideoRepository: AIVideoRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.aiVideoRepository = aiVideoRepository
    super.init(crashReporter: crashReporter)
  }

  override func execute(request: UInt64) async -> Result<String, GenerateVideoStatusError> {
    await aiVideoRepository.getGenerateVideoStatus(for: request)
  }
}
