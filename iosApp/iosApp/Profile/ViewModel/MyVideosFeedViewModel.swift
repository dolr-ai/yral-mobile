//
//  MyVideosFeedViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine
import iosSharedUmbrella

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
  case toggleLikeFailed(Error)
  case finishedLoadingInitialFeeds
  case deleteVideoSuccess([FeedResult])
  case deleteVideoFailed(String)
  case pageEndReached
}

class MyVideosFeedViewModel: FeedViewModelProtocol, ObservableObject {
  let myVideosUseCase: MyVideosUseCaseProtocol
  let deleteVideoUseCase: DeleteVideoUseCaseProtocol
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
    existingFeeds: [FeedResult],
    info: MyVideosFeedInfo
  ) {
    self.myVideosUseCase = myVideosUseCase
    self.deleteVideoUseCase = deleteVideoUseCase
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
        self.feeds.removeAll(where: { $0.postID == deletedVideos.first?.postID })
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
      unifiedEvent = .loadingMoreFeeds
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

  func deleteVideo(request: DeleteVideoRequest) async {
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: DeleteVideoInitiatedEventData(
        pageName: .home,
        videoId: request.videoId
      )
    )
    unifiedEvent = .deleteVideoInitiated
    let result = await deleteVideoUseCase.execute(
      request: DeleteVideoRequest(postId: request.postId, videoId: request.videoId)
    )
    await MainActor.run {
      switch result {
      case .success:
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: VideoDeletedEventData(pageName: .home, videoId: request.videoId)
        )
      case .failure(let failure):
        unifiedEvent = .deleteVideoFailed(errorMessage: failure.localizedDescription)
        unifiedState = .failure(errorMessage: failure.localizedDescription)
      }
    }
  }

  func getCurrentFeedIndex() -> Int {
    return currentIndex
  }

  func report(request: ReportRequest) async { }

  func blockUser(principalId: String) async { }

  func log(event: VideoEventRequest) async {  }

  func socialSignIn(request: SocialProvider) async {  }

  func addSmileyInfo() async { }

  func refreshFeeds() async { }
}
