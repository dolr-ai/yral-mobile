//
//  FeedsViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import Combine

enum FeedsPageState: Equatable {
  case initalized
  case loading
  case successfullyFetched([FeedResult])
  case failure(Error)

  static func == (lhs: FeedsPageState, rhs: FeedsPageState) -> Bool {
    switch (lhs, rhs) {
    case (.initalized, .initalized): return true
    case (.loading, .loading): return true
    case (.successfullyFetched, .successfullyFetched): return true
    default: return false
    }
  }
}

enum FeedsPageEvent: Equatable {
  case loadingMoreFeeds
  case loadedMoreFeeds
  case loadMoreFeedsFailed(Error)
  case toggledLikeSuccessfully(LikeResult)
  case toggleLikeFailed(Error)
  case fetchingInitialFeeds
  case finishedLoadingInitialFeeds

  static func == (lhs: FeedsPageEvent, rhs: FeedsPageEvent) -> Bool {
    switch (lhs, rhs) {
    case (.loadingMoreFeeds, .loadingMoreFeeds): return true
    case (.loadedMoreFeeds, .loadedMoreFeeds): return true
    case (.loadMoreFeedsFailed, .loadMoreFeedsFailed): return true
    case (.toggledLikeSuccessfully, .toggledLikeSuccessfully): return true
    case (.toggleLikeFailed, .toggleLikeFailed): return true
    case (.fetchingInitialFeeds, .fetchingInitialFeeds): return true
    case (.finishedLoadingInitialFeeds, .finishedLoadingInitialFeeds): return true
    default: return false
    }
  }
}

class FeedsViewModel: FeedViewModelProtocol, ObservableObject {
  let initialFeedsUseCase: FetchInitialFeedsUseCaseProtocol
  let moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol
  let likesUseCase: ToggleLikeUseCaseProtocol
  private var currentFeeds = [FeedResult]()
  private var feedPostIDSet = Set<String>()
  private var cancellables = Set<AnyCancellable>()
  private var isFetchingInitialFeeds = false

  @Published var unifiedState: UnifiedFeedState = .initialized
  @Published var unifiedEvent: UnifiedFeedEvent?

  init(
    fetchFeedsUseCase: FetchInitialFeedsUseCaseProtocol,
    moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol,
    likeUseCase: ToggleLikeUseCaseProtocol
  ) {
    self.initialFeedsUseCase = fetchFeedsUseCase
    self.moreFeedsUseCase = moreFeedsUseCase
    self.likesUseCase = likeUseCase
    self.unifiedEvent = .fetchingInitialFeeds
    isFetchingInitialFeeds = true
    initialFeedsUseCase.feedUpdates
      .receive(on: DispatchQueue.main)
      .sink { [weak self] updatedFeed in
        guard let self = self else { return }
        if !Set(updatedFeed.map { $0.postID }).subtracting(feedPostIDSet).isEmpty {
          feedPostIDSet = feedPostIDSet.union(Set(updatedFeed.map { $0.postID }))
          self.currentFeeds += updatedFeed
          self.unifiedState = .success(feeds: updatedFeed)
        }
      }
      .store(in: &cancellables)
  }

  var unifiedStatePublisher: AnyPublisher<UnifiedFeedState, Never> {
    $unifiedState.eraseToAnyPublisher()
  }
  var unifiedEventPublisher: AnyPublisher<UnifiedFeedEvent?, Never> {
    $unifiedEvent.eraseToAnyPublisher()
  }

  @MainActor func fetchFeeds(request: InitialFeedRequest) async {
    unifiedState = .loading
    do {
      let result = await initialFeedsUseCase.execute(request: request)
      isFetchingInitialFeeds = false
      unifiedEvent = .finishedLoadingInitialFeeds
      switch result {
      case .failure(let failure):
        print(failure)
      default: break
      }
    }
  }

  @MainActor func loadMoreFeeds() async {
    unifiedEvent = .loadingMoreFeeds
    unifiedState = .loading
    do {
      let filteredPosts = currentFeeds.map { feed in
        var item = MlFeed_PostItem()
        item.canisterID = feed.canisterID
        item.postID = UInt32(feed.postID) ?? .zero
        item.videoID = feed.videoID
        return item
      }
      let request = MoreFeedsRequest(
        filteredPosts: filteredPosts,
        numResults: FeedsViewController.Constants.initialNumResults,
        feedType: .currentUser
      )
      let result = await moreFeedsUseCase.execute(request: request)
      switch result {
      case .success(let response):
        unifiedEvent = .loadedMoreFeeds
        if !feedPostIDSet.subtracting(Set(response.map { $0.postID })).isEmpty {
          feedPostIDSet = feedPostIDSet.union(Set(response.map { $0.postID }))
          currentFeeds += response
          unifiedState = .success(feeds: response)
        }
      case .failure(let error):
        unifiedEvent = .loadMoreFeedsFailed(errorMessage: error.localizedDescription)
        unifiedState = .failure(errorMessage: error.localizedDescription)
      }
    }
  }

  @MainActor func toggleLike(request: LikeQuery) async {
    do {
      let result = await likesUseCase.execute(request: request)
      switch result {
      case .success(let response):
        currentFeeds[response.index].isLiked = response.status
        let likeCountDifference = response.status ? Int.one : -Int.one
        currentFeeds[response.index].likeCount += likeCountDifference
        unifiedEvent = .toggledLikeSuccessfully(likeResult: response)
        if isFetchingInitialFeeds {
          unifiedEvent = .finishedLoadingInitialFeeds
        }
      case .failure(let error):
        unifiedEvent = .toggleLikeFailed(errorMessage: error.localizedDescription)
      }
    }
  }

  func getCurrentFeedIndex() -> Int {
    return .zero
  }

  func deleteVideo(request: DeleteVideoRequest) async { }
}
