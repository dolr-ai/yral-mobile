//
//  ProfileDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class ProfileDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeProfileView() -> ProfileView {
    ProfileView(viewModel: makeProfileViewModel())
  }

  func makeProfileViewModel() -> ProfileViewModel {
    ProfileViewModel(useCase: ProfileUseCase(profileRepository: makeProfileRepository()))
  }

  func makeProfileRepository() -> ProfileRepository {
    ProfileRepository(httpService: dependencies.httpService, authClient: dependencies.authClient)
  }
}
