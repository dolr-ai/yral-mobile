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

class GetUploadEndpointUseCase: GetUploadEndpointUseCaseProtocol {
  private let uploadRepository: UploadRepositoryProtocol

  init(uploadRepository: UploadRepositoryProtocol) {
    self.uploadRepository = uploadRepository
  }

  func execute() async -> Result<UploadEndpointResponse, VideoUploadError> {
    await uploadRepository.fetchUploadUrl()
  }
}
