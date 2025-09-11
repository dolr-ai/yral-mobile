//
//  GenerateVideoUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol GenerateVideoUseCaseProtocol {
  func execute(request: GenerateVideoMetaRequest) async -> Result<GenerateVideoResponse, GenerateVideoError>
}

class GenerateVideoUseCase: BaseResultUseCase<GenerateVideoMetaRequest, GenerateVideoResponse, GenerateVideoError>,
                            GenerateVideoUseCaseProtocol {
  private let aiVideoRepository: AIVideoRepositoryProtocol

  init(
    aiVideoRepository: AIVideoRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.aiVideoRepository = aiVideoRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: GenerateVideoMetaRequest)
  async -> Result<GenerateVideoResponse, GenerateVideoError> {
    await aiVideoRepository.generateVideo(for: request)
  }
}
