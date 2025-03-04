//
//  UploadDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//
final class UploadDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeUploadView() -> UploadView {
    UploadView(viewModel: makeUploadViewModel())
  }

  func makeUploadViewModel() -> UploadViewModel {
    UploadViewModel(getUploadEndpointUseCase: GetUploadEndpointUseCase(uploadRepository: makeUploadRepository()))
  }

  func makeUploadRepository() -> UploadRepository {
    UploadRepository(httpService: dependencies.httpService, authClient: dependencies.authClient)
  }
}
