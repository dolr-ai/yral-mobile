//
//  FeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Combine

protocol FetchInitialFeedsUseCaseProtocol {
  func execute(request: InitialFeedRequest) async -> Result<Void, FeedError>
  var feedUpdates: AnyPublisher<[FeedResult], Never> { get }
}

class FetchInitialFeedsUseCase:
  BaseResultUseCase<InitialFeedRequest, Void, FeedError>,
    FetchInitialFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol
  var feedUpdates: AnyPublisher<[FeedResult], Never> {
    feedRepository.feedUpdates
  }

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: InitialFeedRequest) async -> Result<Void, FeedError> {
    await feedRepository.getInitialFeeds(numResults: request.numResults)
  }
}
