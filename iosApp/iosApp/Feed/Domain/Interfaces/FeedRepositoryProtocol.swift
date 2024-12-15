//
//  FeedRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
<<<<<<< HEAD
import Combine

protocol FeedRepositoryProtocol {
  func getInitialFeeds(numResults: Int) async throws
  func fetchMoreFeeds(request: MoreFeedsRequest) async throws -> Result<[FeedResult], Error>
  func toggleLikeStatus(for request: LikeQuery) async throws -> Result<LikeResult, Error>
  var feedUpdates: AnyPublisher<[FeedResult], Never> { get }
=======

protocol FeedRepositoryProtocol {
  func fetchFeed(request: FeedRequest) async -> Result<[FeedResult], Error>
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
}
