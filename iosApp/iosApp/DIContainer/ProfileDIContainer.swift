//
//  ProfileDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class ProfileDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
    let crashReporter: CrashReporter
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeAccountRepository() -> AccountRepository {
    AccountRepository(httpService: dependencies.httpService, authClient: dependencies.authClient)
  }

  func makeAccountViewModel() -> ProfileViewModel {
    let accountRepository = makeAccountRepository()
    return ProfileViewModel(
      accountUseCase: AccountUseCase(
        accountRepository: makeAccountRepository(),
        crashReporter: dependencies.crashReporter
      ),
      myVideosUseCase: MyVideosUseCase(
        accountRepository: accountRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeProfileView() -> ProfileView {
    ProfileView(viewModel: makeAccountViewModel())
  }
}
