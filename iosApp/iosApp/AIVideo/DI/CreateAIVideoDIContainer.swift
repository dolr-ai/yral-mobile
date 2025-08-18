//
//  CreateAIVideoDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class CreateAIVideoDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let crashReporter: CrashReporter
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeAIVideoRepository() -> AIVideoRepository {
    AIVideoRepository(httpService: dependencies.httpService)
  }

  func makeAIVideoViewModel() -> CreateAIVideoViewModel {
    let aiVideoRepository = makeAIVideoRepository()
    return CreateAIVideoViewModel(
      aiVideoProviderUseCase: AIVideoProviderUseCase(
        aiVideoRepository: aiVideoRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeCreateAIVideoSreenView(onDismiss: @escaping () -> Void) -> CreateAIVideoScreenView {
    return CreateAIVideoScreenView(
      viewModel: makeAIVideoViewModel(), onDismiss: onDismiss)
  }
}
