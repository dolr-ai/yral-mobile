//
//  FetchMoreFeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 06/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol FetchMoreFeedsUseCaseProtocol {
  func execute(request: MoreFeedsRequest) async throws -> Result<[FeedResult], FeedError>
}

class FetchMoreFeedsUseCase:
  BaseResultUseCase<MoreFeedsRequest, [FeedResult], FeedError>,
  FetchMoreFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: MoreFeedsRequest) async -> Result<[FeedResult], FeedError> {
    return await feedRepository.fetchMoreFeeds(request: request)
  }
}
