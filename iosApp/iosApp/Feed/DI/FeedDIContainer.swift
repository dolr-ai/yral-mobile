//
//  FeedDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import SwiftUI

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

  func makeFeedsViewControllerWrapper(showFeeds: Binding<Bool>) -> FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(
      feedsViewController: FeedsViewController(viewModel: makeFeedsViewModel()),
      showFeeds: showFeeds
    )
  }

  func makeFeedsViewController() -> FeedsViewController {
    FeedsViewController(viewModel: makeFeedsViewModel())
  }

  func makeFeedsViewModel() -> FeedsViewModel {
    let repository = FeedsRepository(
      httpService: dependencies.httpService,
      mlClient: dependencies.mlfeedService,
      authClient: dependencies.authClient
    )
    return FeedsViewModel(
      fetchFeedsUseCase: FetchInitialFeedsUseCase(
        feedRepository: repository,
        crashReporter: dependencies.crashReporter
      ),
      moreFeedsUseCase: FetchMoreFeedsUseCase(
        feedRepository: repository,
        crashReporter: dependencies.crashReporter
      ),
      reportUseCase: ReportFeedsUseCase(
        feedRepository: repository,
        crashReporter: dependencies.crashReporter
      ),
      logEventUseCase: LogUploadEventUseCase(
        feedRepository: repository,
        crashReporter: dependencies.crashReporter
      )
    )
  }
}
