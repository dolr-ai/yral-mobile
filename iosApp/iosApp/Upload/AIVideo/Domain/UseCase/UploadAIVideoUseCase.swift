//
//  UploadAIVideoUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol UploadAIVideoUseCaseProtocol {
  func execute(request: String) async -> Result<Void, UploadAIVideoError>
}

class UploadAIVideoUseCase: BaseResultUseCase<String, Void, UploadAIVideoError>, UploadAIVideoUseCaseProtocol {
  private let aiVideoRepository: AIVideoRepositoryProtocol

  init(
    aiVideoRepository: AIVideoRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.aiVideoRepository = aiVideoRepository
    super.init(crashReporter: crashReporter)
  }

  override func execute(request: String) async -> Result<Void, UploadAIVideoError> {
    await aiVideoRepository.uploadVideo(with: request)
  }
}
