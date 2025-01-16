//
//  FetchMoreFeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 06/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol FetchMoreFeedsUseCaseProtocol {
  func execute(request: MoreFeedsRequest) async throws -> Result<[FeedResult], Error>
}

class FetchMoreFeedsUseCase: FetchMoreFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol) {
    self.feedRepository = feedRepository
  }

  func execute(request: MoreFeedsRequest) async throws -> Result<[FeedResult], Error> {
    return try await feedRepository.fetchMoreFeeds(request: request)
  }
}
