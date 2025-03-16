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

class UploadVideoUseCase:
  BaseStreamingUseCase<UploadVideoRequest, Double, VideoUploadError>,
    UploadVideoUseCaseProtocol {
  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol, crashReporter: CrashReporter) {
    self.uploadRepository = uploadRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: UploadVideoRequest) -> AsyncThrowingStream<Double, any Error> {
    return uploadRepository.uploadVideoWithProgress(request: request)
  }

  override func convertToDomainError(_ error: any Error) -> VideoUploadError {
    if let domainErr = error as? VideoUploadError {
      return domainErr
    }
    return VideoUploadError.unknown(error)
  }
}
