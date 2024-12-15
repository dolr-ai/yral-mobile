//
//  FeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

protocol FeedsUseCaseProtocol {
  func execute(request: FeedRequest) async throws -> Result<[FeedResult], Error>
}

class FeedsUseCase: FeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol) {
    self.feedRepository = feedRepository
  }

  func execute(request: FeedRequest) async throws -> Result<[FeedResult], Error> {
    await feedRepository.fetchFeed(request: request)
  }
}
