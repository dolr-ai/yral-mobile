//
//  FeedsViewModelProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine

// ADDED: Unified types used by all feed view models.
enum UnifiedFeedState {
  case initialized
  case loading
  case success(feeds: [FeedResult])
  case failure(errorMessage: String)
}

enum UnifiedFeedEvent: Equatable {
  case fetchingInitialFeeds
  case loadingMoreFeeds
  case loadedMoreFeeds
  case loadMoreFeedsFailed(errorMessage: String)
  case toggledLikeSuccessfully(likeResult: LikeResult)
  case toggleLikeFailed(errorMessage: String)
  case finishedLoadingInitialFeeds
  case deleteVideoInitiated
  case deleteVideoSuccess(feeds: [FeedResult])
  case deleteVideoFailed(errorMessage: String)
  case pageEndReached
  case reportInitiated
  case reportSuccess(String)
  case reportFailed(Error)
  case blockedUser(String)

  // swiftlint: disable cyclomatic_complexity
  static func == (lhs: UnifiedFeedEvent, rhs: UnifiedFeedEvent) -> Bool {
    switch (lhs, rhs) {
    case (.fetchingInitialFeeds, .fetchingInitialFeeds):
      return true
    case (.loadingMoreFeeds, .loadingMoreFeeds):
      return true
    case (.loadedMoreFeeds, .loadedMoreFeeds):
      return true
    case (.loadMoreFeedsFailed(let lhsMsg), .loadMoreFeedsFailed(let rhsMsg)):
      return lhsMsg == rhsMsg
    case (.toggledLikeSuccessfully(let lhsResult), .toggledLikeSuccessfully(let rhsResult)):
      return lhsResult == rhsResult
    case (.toggleLikeFailed(let lhsMsg), .toggleLikeFailed(let rhsMsg)):
      return lhsMsg == rhsMsg
    case (.finishedLoadingInitialFeeds, .finishedLoadingInitialFeeds):
      return true
    case (.deleteVideoSuccess(let lhsFeeds), .deleteVideoSuccess(let rhsFeeds)):
      return lhsFeeds == rhsFeeds
    case (.deleteVideoFailed(let lhsMsg), .deleteVideoFailed(let rhsMsg)):
      return lhsMsg == rhsMsg
    case (.pageEndReached, .pageEndReached):
      return true
    default:
      return false
    }
  }
  // swiftlint: enable cyclomatic_complexity
}

protocol FeedViewModelProtocol: ObservableObject {
  var unifiedStatePublisher: AnyPublisher<UnifiedFeedState, Never> { get }
  var unifiedEventPublisher: AnyPublisher<UnifiedFeedEvent?, Never> { get }
  var unifiedEvent: UnifiedFeedEvent? { get }

  func fetchFeeds(request: InitialFeedRequest) async
  func loadMoreFeeds() async
  func toggleLike(request: LikeQuery) async
  func deleteVideo(request: DeleteVideoRequest) async
  func getCurrentFeedIndex() -> Int
  func report(request: ReportRequest) async
  func blockUser(principalId: String) async
}
