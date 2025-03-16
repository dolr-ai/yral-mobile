//
//  FeedRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Combine

protocol FeedRepositoryProtocol {
  func getInitialFeeds(numResults: Int) async -> Result<Void, FeedError>
  func fetchMoreFeeds(request: MoreFeedsRequest) async -> Result<[FeedResult], FeedError>
  func toggleLikeStatus(for request: LikeQuery) async -> Result<LikeResult, FeedError>
  var feedUpdates: AnyPublisher<[FeedResult], Never> { get }
}
