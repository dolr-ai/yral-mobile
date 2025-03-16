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
      useCase: AccountUseCase(
        accountRepository: makeAccountRepository(),
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeAccountRepository() -> AccountRepository {
    AccountRepository(httpService: dependencies.httpService, authClient: dependencies.authClient)
  }
}
