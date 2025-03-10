//
//  UploadUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol UploadVideoUseCaseProtocol {
  func execute(request: UploadVideoRequest) async -> AsyncThrowingStream<Double, Error>
}

class UploadVideoUseCase: UploadVideoUseCaseProtocol {
  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol) {
    self.uploadRepository = uploadRepository
  }

  func execute(request: UploadVideoRequest) -> AsyncThrowingStream<Double, Error> {
    uploadRepository.uploadVideoWithProgress(request: request)
  }
}
