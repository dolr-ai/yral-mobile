//
//  FeedDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

final class FeedDIContainer {
  struct Dependencies {
    let mlfeedService: MlFeed_MLFeedNIOClient
    let httpService: HTTPService
    let authClient: AuthClient
    let crashReporter: CrashReporter
    let feedsRepository: FeedsRepository
    let fetchMoreFeedsUseCase: FetchMoreFeedsUseCaseProtocol
    let toggleLikeUseCase: ToggleLikeUseCaseProtocol
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeFeedsViewControllerWrapper() -> FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(feedsViewController: FeedsViewController(viewModel: makeFeedsViewModel()))
  }

  func makeFeedsViewModel() -> FeedsViewModel {
    return FeedsViewModel(
      fetchFeedsUseCase: FetchInitialFeedsUseCase(
        feedRepository: dependencies.feedsRepository,
        crashReporter: dependencies.crashReporter
      ),
      moreFeedsUseCase: dependencies.fetchMoreFeedsUseCase,
      likeUseCase: dependencies.toggleLikeUseCase
    )
  }
}
