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
    let accountRepository: AccountRepository
    let accountUseCase: AccountUseCase
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeAccountViewModel() -> ProfileViewModel {
    return ProfileViewModel(
      accountUseCase: dependencies.accountUseCase,
      myVideosUseCase: MyVideosUseCase(
        profileRepository: ProfileRepository(
          httpService: dependencies.httpService,
          authClient: dependencies.authClient
        ),
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeProfileView() -> ProfileView {
    ProfileView(viewModel: makeAccountViewModel())
  }
}
