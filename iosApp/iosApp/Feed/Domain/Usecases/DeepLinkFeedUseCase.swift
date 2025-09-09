//
//  FeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Combine

protocol DeepLinkFeedUseCaseProtocol {
  func execute(request: DeepLinkFeedRequest) async -> Result<FeedResult, FeedError>
}

class DeepLinkFeedUseCase:
  BaseResultUseCase<DeepLinkFeedRequest, FeedResult, FeedError>,
  DeepLinkFeedUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: DeepLinkFeedRequest) async -> Result<FeedResult, FeedError> {
    await feedRepository.fetchDeepLinkFeed(request: request)
  }
}
