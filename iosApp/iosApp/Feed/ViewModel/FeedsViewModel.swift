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

enum FeedsPageEvent {
  case loadedMoreFeeds
  case loadMoreFeedsFailed(Error)
  case toggledLikeSuccessfully(LikeResult)
  case toggleLikeFailed(Error)
  case fetchingInitialFeeds
  case finishedLoadingInitialFeeds

  static func == (lhs: FeedsPageEvent, rhs: FeedsPageEvent) -> Bool {
    switch (lhs, rhs) {
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

class FeedsViewModel: ObservableObject {
  let initialFeedsUseCase: FetchInitialFeedsUseCase
  let moreFeedsUseCase: FetchMoreFeedsUseCase
  let likesUseCase: ToggleLikeUseCase
  private var currentFeeds = [FeedResult]()
  private var feedPostIDSet = Set<String>()
  private var cancellables = Set<AnyCancellable>()
  private var isFetchingInitialFeeds = false

  @Published var state: FeedsPageState = .initalized
  @Published var event: FeedsPageEvent?

  init(
    fetchFeedsUseCase: FetchInitialFeedsUseCase,
    moreFeedsUseCase: FetchMoreFeedsUseCase,
    likeUseCase: ToggleLikeUseCase
  ) {
    self.initialFeedsUseCase = fetchFeedsUseCase
    self.moreFeedsUseCase = moreFeedsUseCase
    self.likesUseCase = likeUseCase
    self.event = .fetchingInitialFeeds
    isFetchingInitialFeeds = true
    initialFeedsUseCase.feedUpdates
      .receive(on: DispatchQueue.main)
      .sink { [weak self] updatedFeed in
        guard let self else { return }
        if !Set(updatedFeed.map { $0.postID }).subtracting(feedPostIDSet).isEmpty {
          feedPostIDSet = feedPostIDSet.union(Set(updatedFeed.map { $0.postID }))
          self.currentFeeds += updatedFeed
          self.state = .successfullyFetched(updatedFeed)
        }
      }
      .store(in: &cancellables)
  }

  @MainActor func fetchFeeds(request: InitialFeedRequest) async {
    state = .loading
    do {
      try await initialFeedsUseCase.execute(request: request)
      isFetchingInitialFeeds = false
      event = .finishedLoadingInitialFeeds
    } catch {
      state = .failure(error)
    }
  }

  @MainActor func loadMoreFeeds() async {
    state  = .loading
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
        numResults: FeedsViewController.Constants.initialNumResults
      )
      let result = try await moreFeedsUseCase.execute(request: request)
      switch result {
      case .success(let response):
        event = .loadedMoreFeeds
        if !feedPostIDSet.subtracting(Set(response.map { $0.postID })).isEmpty {
          feedPostIDSet = feedPostIDSet.union(Set(response.map { $0.postID }))
          currentFeeds += response
          state = .successfullyFetched(response)
        }
      case .failure(let error):
        event = .loadMoreFeedsFailed(error)
        state = .failure(error)
      }
    } catch {
      event = .loadMoreFeedsFailed(error)
      state = .failure(error)
    }
  }

  @MainActor func toggleLike(request: LikeQuery) async {
    do {
      let result = try await likesUseCase.execute(request: request)
      switch result {
      case .success(let response):
        currentFeeds[response.index].isLiked = response.status
        let likeCountDifference = response.status ? Int.one : -Int.one
        currentFeeds[response.index].likeCount += likeCountDifference
        event = .toggledLikeSuccessfully(response)
        if isFetchingInitialFeeds {
          event = .fetchingInitialFeeds
        }
      case .failure(let error):
        event = .toggleLikeFailed(error)
      }
    } catch {
      event = .toggleLikeFailed(error)
    }
  }
}
