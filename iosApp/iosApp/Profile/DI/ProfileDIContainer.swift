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
    let accountUseCase: AccountUseCaseProtocol
    let likesUseCase: ToggleLikeUseCaseProtocol
  }

  private let dependencies: Dependencies

  lazy var profileRepository: ProfileRepositoryProtocol = {
    ProfileRepository(
      httpService: dependencies.httpService,
      authClient: dependencies.authClient
    )
  }()

  lazy var myVideosUseCase: MyVideosUseCaseProtocol = {
    MyVideosUseCase(
      profileRepository: profileRepository,
      crashReporter: dependencies.crashReporter
    )
  }()

  lazy var deleteVideosUseCase: DeleteVideoUseCaseProtocol = {
    DeleteVideoUseCase(
      profileRepository: profileRepository,
      crashReporter: dependencies.crashReporter
    )
  }()

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  private func makeProfileViewModel() -> ProfileViewModel {
    return ProfileViewModel(
      accountUseCase: dependencies.accountUseCase,
      myVideosUseCase: myVideosUseCase,
      deleteVideoUseCase: deleteVideosUseCase
    )
  }

  private func makeMyVideosFeedViewModel(existingFeeds: [FeedResult], info: MyVideosFeedInfo) -> MyVideosFeedViewModel {
    return MyVideosFeedViewModel(
      myVideosUseCase: myVideosUseCase,
      deleteVideoUseCase: deleteVideosUseCase,
      likeVideoUseCase: dependencies.likesUseCase,
      existingFeeds: existingFeeds,
      info: info
    )
  }

  func makeProfileView() -> ProfileView {
    let router = ProfileRouter(profileDI: self)
    return ProfileView(
      viewModel: makeProfileViewModel(),
      router: router
    )
  }

  func makeMyVideosView(existingFeeds: [FeedResult], info: MyVideosFeedInfo) -> FeedsViewControllerWrapper {
    let viewController = FeedsViewController(
      viewModel: makeMyVideosFeedViewModel(
        existingFeeds: existingFeeds,
        info: info
      ),
      feedType: .currentUser
    )
    return FeedsViewControllerWrapper(feedsViewController: viewController)
  }
}
