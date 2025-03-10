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

class UpdateMetaUseCase: UpdateMetaUseCaseProtocol {
  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol) {
    self.uploadRepository = uploadRepository
  }

  func execute(request: UploadVideoRequest) async -> Result<Void, VideoUploadError> {
    let upstream = uploadRepository.uploadVideoWithProgress(request: request)
    return await self.uploadRepository.updateMetadata(request: request)
  }
}
