//
//  FeedsViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import Combine

class FeedsViewModel: FeedViewModelProtocol, ObservableObject {
  let initialFeedsUseCase: FetchInitialFeedsUseCaseProtocol
  let moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol
  let likesUseCase: ToggleLikeUseCaseProtocol
  let reportUseCase: ReportFeedsUseCaseProtocol
  private var currentFeeds = [FeedResult]()
  private var feedvideoIDSet = Set<String>()
  private var cancellables = Set<AnyCancellable>()
  private var isFetchingInitialFeeds = false

  @Published var unifiedState: UnifiedFeedState = .initialized
  @Published var unifiedEvent: UnifiedFeedEvent?

  init(
    fetchFeedsUseCase: FetchInitialFeedsUseCaseProtocol,
    moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol,
    likeUseCase: ToggleLikeUseCaseProtocol,
    reportUseCase: ReportFeedsUseCaseProtocol
  ) {
    self.initialFeedsUseCase = fetchFeedsUseCase
    self.moreFeedsUseCase = moreFeedsUseCase
    self.likesUseCase = likeUseCase
    self.reportUseCase = reportUseCase
    self.unifiedEvent = .fetchingInitialFeeds
    isFetchingInitialFeeds = true

    initialFeedsUseCase.feedUpdates
      .receive(on: DispatchQueue.main)
      .sink { [weak self] updatedFeed in
        guard let self = self else { return }
        let newFeeds = updatedFeed.filter { !self.feedvideoIDSet.contains($0.videoID) }
        guard !newFeeds.isEmpty else { return }
        self.feedvideoIDSet.formUnion(newFeeds.map { $0.videoID })
        self.currentFeeds += newFeeds
        self.unifiedState = .success(feeds: newFeeds)
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
      let filteredPosts = currentFeeds.map {
        FilteredPosts(
          postID: $0.postID,
          canisterID: $0.canisterID,
          videoID: $0.videoID,
          nsfwProbability: $0.nsfwProbability
        )
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
        var tempSet = Set<String>()
        let uniqueInBatch = response.filter { feed in
          guard !tempSet.contains(feed.videoID) else { return false }
          tempSet.insert(feed.videoID)
          return true
        }
        let newFeeds = uniqueInBatch.filter { !feedvideoIDSet.contains($0.videoID) }
        guard !newFeeds.isEmpty else { return }

        feedvideoIDSet.formUnion(newFeeds.map { $0.videoID })
        currentFeeds += newFeeds
        unifiedState = .success(feeds: newFeeds)

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

  @MainActor func report(request: ReportRequest) async {
    unifiedEvent = .reportInitiated
    let result = await reportUseCase.execute(request: request)
    switch result {
    case .success(let postID):
      unifiedEvent = .reportSuccess(postID)
    case .failure(let failure):
      unifiedEvent = .reportFailed(failure)
      print(failure.localizedDescription)
    }
  }

  func getCurrentFeedIndex() -> Int {
    return .zero
  }

  func deleteVideo(request: DeleteVideoRequest) async { }
  }
