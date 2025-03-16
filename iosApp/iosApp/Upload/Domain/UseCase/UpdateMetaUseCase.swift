//
//  UpdateMetaUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 07/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol UpdateMetaUseCaseProtocol {
  func execute(request: UploadVideoRequest) async -> Result<Void, VideoUploadError>
}

class UpdateMetaUseCase:
  BaseResultUseCase<UploadVideoRequest, Void, VideoUploadError>,
    UpdateMetaUseCaseProtocol {
  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol, crashReporter: CrashReporter) {
    self.uploadRepository = uploadRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: UploadVideoRequest) async -> Result<Void, VideoUploadError> {
    return await self.uploadRepository.updateMetadata(request: request)
  }
}
