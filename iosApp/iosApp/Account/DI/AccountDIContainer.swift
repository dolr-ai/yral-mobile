//
//  ProfileDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class AccountDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
    let crashReporter: CrashReporter
    let accountUseCase: AccountUseCaseProtocol
    let accountRepository: AccountRepositoryProtocol
    let socialSignInUseCase: SocialSignInUseCaseProtocol
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeAccountView() -> AccountView {
    AccountView(viewModel: makeAccountViewModel())
  }

  func makeAccountViewModel() -> AccountViewModel {
    AccountViewModel(
      accountUseCase: dependencies.accountUseCase,
      socialSignInUseCase: dependencies.socialSignInUseCase,
      logoutUseCase: LogoutUseCase(
        accountRepository: dependencies.accountRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }
}
