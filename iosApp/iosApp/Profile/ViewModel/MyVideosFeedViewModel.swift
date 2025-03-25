//
//  MyVideosFeedViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine

enum MyVideosPageState {
  case initalized
  case loading
  case successfullyFetched([FeedResult])
  case failure(String)
}

enum MyVideosPageEvent {
  case loadingMoreFeeds
  case loadedMoreFeeds
  case loadMoreFeedsFailed(Error)
  case toggledLikeSuccessfully(LikeResult)
  case toggleLikeFailed(Error)
  case finishedLoadingInitialFeeds
  case deleteVideoSuccess([FeedResult])
  case deleteVideoFailed(String)
  case pageEndReached
}

class MyVideosFeedViewModel: FeedViewModelProtocol, ObservableObject {
  let myVideosUseCase: MyVideosUseCaseProtocol
  let deleteVideoUseCase: DeleteVideoUseCaseProtocol
  let likeVideoUseCase: ToggleLikeUseCaseProtocol
  var feeds: [FeedResult]
  var startIndex = Int.zero
  var currentIndex = Int.zero
  var offset = ProfileRepository.Constants.offset
  private var isLoading = false
  private(set) var hasMorePages = true

  @Published var unifiedState: UnifiedFeedState = .initialized
  @Published var unifiedEvent: UnifiedFeedEvent?
  private var cancellables = Set<AnyCancellable>()

  init(
    myVideosUseCase: MyVideosUseCaseProtocol,
    deleteVideoUseCase: DeleteVideoUseCaseProtocol,
    likeVideoUseCase: ToggleLikeUseCaseProtocol,
    existingFeeds: [FeedResult],
    info: MyVideosFeedInfo
  ) {
    self.myVideosUseCase = myVideosUseCase
    self.deleteVideoUseCase = deleteVideoUseCase
    self.likeVideoUseCase = likeVideoUseCase
    self.feeds = existingFeeds
    self.startIndex = info.startIndex
    self.currentIndex = info.currentIndex

    myVideosUseCase.newVideosPublisher
      .receive(on: RunLoop.main)
      .sink { [weak self] newVideos in
        guard let self = self else { return }
        self.unifiedEvent = .loadedMoreFeeds
        self.feeds += newVideos
        self.unifiedState = .success(feeds: newVideos)
      }
      .store(in: &cancellables)

    deleteVideoUseCase.deletedVideoPublisher
      .receive(on: RunLoop.main)
      .sink { [weak self] deletedVideos in
        guard let self = self else { return }
        self.unifiedEvent = .deleteVideoSuccess(feeds: deletedVideos)
      }
      .store(in: &cancellables)
  }

  var unifiedStatePublisher: AnyPublisher<UnifiedFeedState, Never> {
    $unifiedState.eraseToAnyPublisher()
  }
  var unifiedEventPublisher: AnyPublisher<UnifiedFeedEvent?, Never> {
    $unifiedEvent.eraseToAnyPublisher()
  }

  func fetchFeeds(request: InitialFeedRequest) async {
    unifiedState = .success(feeds: feeds)
    self.unifiedEvent = .finishedLoadingInitialFeeds
  }

  func loadMoreFeeds() async {
    guard !isLoading, hasMorePages else { return }

    await MainActor.run {
      unifiedState = .loading
      isLoading = true
    }
    let result = await myVideosUseCase.execute(
      request: ProfileVideoRequest(
        startIndex: UInt64(startIndex),
        offset: UInt64(offset)
      )
    )
    await MainActor.run {
      switch result {
      case .success:
        unifiedState = .success(feeds: feeds)
        startIndex += offset
      case .failure(let error):
        switch error {
        case .pageEndReached:
          unifiedEvent = .pageEndReached
          unifiedState = .success(feeds: feeds)
          hasMorePages = false
        default:
          unifiedState = .failure(errorMessage: error.localizedDescription)
        }
      }
      isLoading = false
    }
  }

  func toggleLike(request: LikeQuery) async {
    do {
      let result = await likeVideoUseCase.execute(request: request)
      switch result {
      case .success(let response):
        feeds[response.index].isLiked = response.status
        let likeCountDifference = response.status ? Int.one : -Int.one
        feeds[response.index].likeCount += likeCountDifference
        unifiedEvent = .toggledLikeSuccessfully(likeResult: response)
      case .failure(let error):
        unifiedEvent = .toggleLikeFailed(errorMessage: error.localizedDescription)
      }
    }
  }

  func deleteVideo(request: DeleteVideoRequest) async {
    let result = await deleteVideoUseCase.execute(
      request: DeleteVideoRequest(postId: request.postId, videoId: request.videoId)
    )
    await MainActor.run {
      switch result {
      case .success:
        unifiedState = .success(feeds: feeds)
      case .failure(let failure):
        unifiedEvent = .deleteVideoFailed(errorMessage: failure.localizedDescription)
        unifiedState = .failure(errorMessage: failure.localizedDescription)
      }
    }
  }
}
