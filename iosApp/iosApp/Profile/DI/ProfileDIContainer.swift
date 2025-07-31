//
//  ProfileDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

final class ProfileDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
    let crashReporter: CrashReporter
    let accountUseCase: AccountUseCaseProtocol
    let session: SessionManager
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

  @MainActor private func makeProfileViewModel() -> ProfileViewModel {
    return ProfileViewModel(
      accountUseCase: dependencies.accountUseCase,
      myVideosUseCase: myVideosUseCase,
      deleteVideoUseCase: deleteVideosUseCase,
      refreshVideoUseCase: RefreshVideosUseCase(
        profileRepository: profileRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  private func makeMyVideosFeedViewModel(existingFeeds: [FeedResult], info: MyVideosFeedInfo) -> MyVideosFeedViewModel {
    return MyVideosFeedViewModel(
      myVideosUseCase: myVideosUseCase,
      deleteVideoUseCase: deleteVideosUseCase,
      existingFeeds: existingFeeds,
      info: info
    )
  }

  @MainActor func makeProfileView() -> ProfileView {
    let router = ProfileRouter(profileDI: self)
    return ProfileView(
      viewModel: makeProfileViewModel(),
      router: router
    )
  }

  func makeMyVideosView(
    existingFeeds: [FeedResult],
    info: MyVideosFeedInfo,
    showFeeds: Binding<Bool>,
    walletPhase: Binding<WalletPhase>,
    walletOutcome: Binding<WalletPhase>
  ) -> FeedsViewControllerWrapper {
    return FeedsViewControllerWrapper(
      feedsViewController: FeedsViewController(
        viewModel: makeMyVideosFeedViewModel(
          existingFeeds: existingFeeds,
          info: info
        ),
        feedType: .currentUser,
        session: dependencies.session,
        crashReporter: dependencies.crashReporter
      ),
      showFeeds: showFeeds,
      walletPhase: walletPhase,
      walletOutcome: walletOutcome
    )
  }
}
