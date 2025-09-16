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
    let firebaseService: FirebaseServiceProtocol
    let authClient: AuthClient
    let crashReporter: CrashReporter
    let socialSignInUseCase: SocialSignInUseCaseProtocol
    let session: SessionManager
    let eventBus: EventBus
    let castVoteUseCase: CastVoteUseCaseProtocol
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeFeedsViewController(playToScroll: Bool) -> FeedsViewController {
    FeedsViewController(
      viewModel: makeFeedsViewModel(),
      playToScroll: playToScroll,
      session: dependencies.session,
      eventBus: dependencies.eventBus,
      crashReporter: dependencies.crashReporter
    )
  }

  func makeFeedsViewModel() -> FeedsViewModel {
    let repository = FeedsRepository(
      httpService: dependencies.httpService,
      firebaseService: dependencies.firebaseService,
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
      deeplinkFeedUseCase: DeepLinkFeedUseCase(
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
      ),
      socialSignInUseCase: dependencies.socialSignInUseCase,
      castVoteUseCase: dependencies.castVoteUseCase,
      rechargeWalletUseCase: RechargeUseCase(
        feedRepository: repository,
        crashReporter: dependencies.crashReporter
      )
    )
  }
}
