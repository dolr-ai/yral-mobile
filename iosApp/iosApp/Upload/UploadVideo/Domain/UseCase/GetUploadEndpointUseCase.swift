//
//  UploadUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol GetUploadEndpointUseCaseProtocol {
  func execute() async -> Result<UploadEndpointResponse, VideoUploadError>
}

class GetUploadEndpointUseCase:
  BaseResultUseCase<Void, UploadEndpointResponse, VideoUploadError>,
  GetUploadEndpointUseCaseProtocol {

  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol, crashReporter: CrashReporter) {
    self.uploadRepository = uploadRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<UploadEndpointResponse, VideoUploadError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<UploadEndpointResponse, VideoUploadError> {
    await uploadRepository.fetchUploadUrl()
  }
}
