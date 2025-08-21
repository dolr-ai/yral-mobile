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
    let crashReporter: CrashReporter
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeUploadRepository() -> UploadRepository {
    UploadRepository(httpService: dependencies.httpService, authClient: dependencies.authClient)
  }

  func makeUploadViewModel() -> UploadViewModel {
    let uploadRepository = makeUploadRepository()
    return UploadViewModel(
      getUploadEndpointUseCase: GetUploadEndpointUseCase(
        uploadRepository: uploadRepository,
        crashReporter: dependencies.crashReporter
      ),
      uploadVideoUseCase: UploadVideoUseCase(
        uploadRepository: uploadRepository,
        crashReporter: dependencies.crashReporter
      ),
      updateMetaUseCase: UpdateMetaUseCase(
        uploadRepository: uploadRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeUploadView(onDismiss: @escaping () -> Void) -> UploadView {
    UploadView(viewModel: makeUploadViewModel(), onDismiss: onDismiss)
  }
}
