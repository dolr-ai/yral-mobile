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
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeFeedsViewControllerWrapper() -> FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(feedsViewController: FeedsViewController(viewModel: makeFeedsViewModel()))
  }

  func makeFeedsViewModel() -> FeedsViewModel {
    let feedRepository = makeFeedsRepository()
    return FeedsViewModel(
      fetchFeedsUseCase: FetchInitialFeedsUseCase(
        feedRepository: feedRepository,
        crashReporter: dependencies.crashReporter
      ),
      moreFeedsUseCase: FetchMoreFeedsUseCase(feedRepository: feedRepository,
                                              crashReporter: dependencies.crashReporter
                                             ),
      likeUseCase: ToggleLikeUseCase(
        feedRepository: feedRepository,
        crashReporter: dependencies.crashReporter
      )
    )
  }

  func makeFeedsRepository() -> FeedsRepository {
    FeedsRepository(
      httpService: dependencies.httpService,
      mlClient: dependencies.mlfeedService,
      authClient: dependencies.authClient
    )
  }
}
