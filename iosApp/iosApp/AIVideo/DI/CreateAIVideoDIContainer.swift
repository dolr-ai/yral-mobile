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
    let authClient: AuthClient
    let crashReporter: CrashReporter
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeAIVideoRepository() -> AIVideoRepository {
    AIVideoRepository(
      httpService: dependencies.httpService,
      authClient: dependencies.authClient
    )
  }

  func makeAccountRepository() -> AccountRepository {
    AccountRepository(
      httpService: dependencies.httpService,
      authClient: dependencies.authClient
    )
  }

  func makeAIVideoViewModel() -> CreateAIVideoViewModel {
    let aiVideoRepository = makeAIVideoRepository()
    let accountRepository = makeAccountRepository()

    return CreateAIVideoViewModel(
      aiVideoProviderUseCase: AIVideoProviderUseCase(
        aiVideoRepository: aiVideoRepository,
        crashReporter: dependencies.crashReporter
      ),
      rateLimitStatusUseCase: RateLimitStatusUseCase(
        aiVideoRepository: aiVideoRepository,
        crashReporter: dependencies.crashReporter
      ),
      socialSigninUseCase: SocialSignInUseCase(
        accountRepository: accountRepository,
        crashReporter: dependencies.crashReporter
      ),
      generateVideoUseCase: GenerateVideoUseCase(
        aiVideoRepository: aiVideoRepository,
        crashReporter: dependencies.crashReporter
      ),
      generateVideoStatusUseCase: GenerateVideoStatusUseCase(
        aiVideoRepository: aiVideoRepository,
        crashReporter: dependencies.crashReporter
      ),
      uploadAIVideoUseCase: UploadAIVideoUseCase(
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
