//
//  RateLimitStatusUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 20/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol RateLimitStatusUseCaseProtocol {
  func execute() async -> Result<RateLimitStatus, RateLimitStatusError>
}

class RateLimitStatusUseCase: BaseResultUseCase<
Void, RateLimitStatus, RateLimitStatusError>,
RateLimitStatusUseCaseProtocol {
  private let aiVideoRepository: AIVideoRepositoryProtocol

  init(
    aiVideoRepository: AIVideoRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.aiVideoRepository = aiVideoRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<RateLimitStatus, RateLimitStatusError> {
    await aiVideoRepository.getRateLimitStatus()
  }
}
