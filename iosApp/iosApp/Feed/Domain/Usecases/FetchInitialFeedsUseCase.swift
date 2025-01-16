//
//  FeedsUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Combine

protocol FetchInitialFeedsUseCaseProtocol {
  func execute(request: InitialFeedRequest) async throws
  var feedUpdates: AnyPublisher<[FeedResult], Never> { get }
}

class FetchInitialFeedsUseCase: FetchInitialFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol
  var feedUpdates: AnyPublisher<[FeedResult], Never> {
    feedRepository.feedUpdates
  }

  init(feedRepository: FeedRepositoryProtocol) {
    self.feedRepository = feedRepository
  }

  func execute(request: InitialFeedRequest) async throws {
    try await withCheckedThrowingContinuation { continuation in
      Task {
        do {
          try await feedRepository.getInitialFeeds(numResults: request.numResults)
          continuation.resume()
        } catch {
          continuation.resume(throwing: error)
        }
      }
    }
  }

  func fetchMoreFeeds(request: MoreFeedsRequest) async throws -> Result<[FeedResult], Error> {
    return try await feedRepository.fetchMoreFeeds(request: request)
  }
}
