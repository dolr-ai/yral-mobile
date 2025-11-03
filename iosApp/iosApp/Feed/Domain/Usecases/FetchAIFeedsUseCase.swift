//
//  FetchAIFeedsUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 03/11/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Combine

protocol FetchAIFeedsUseCaseProtocol {
  func execute(request: Int) async -> Result<[FeedResult], FeedError>
  var feedUpdates: AnyPublisher<[FeedResult], Never> { get }
}

class FetchAIFeedsUseCase: BaseResultUseCase<Int, [FeedResult], FeedError>, FetchAIFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol
  var feedUpdates: AnyPublisher<[FeedResult], Never> {
    feedRepository.feedUpdates
  }

  init(
    feedRepository: FeedRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: Int) async -> Result<[FeedResult], FeedError> {
    await feedRepository.getAIFeeds(count: request)
  }
}
