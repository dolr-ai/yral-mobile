//
//  FeedsViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import Combine
import iosSharedUmbrella

class FeedsViewModel: FeedViewModelProtocol, ObservableObject {
  let initialFeedsUseCase: FetchInitialFeedsUseCaseProtocol
  let moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol
  let reportUseCase: ReportFeedsUseCaseProtocol
  let logEventUseCase: LogUploadEventUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol
  let castVoteUseCase: CastVoteUseCaseProtocol
  private var currentFeeds = [FeedResult]()
  private var filteredFeeds = [FeedResult]()
  private var feedvideoIDSet = Set<String>()
  private var blockedPrincipalIDSet: Set<String> = {
    do {
      return try KeychainHelper.retrieveSet(for: Constants.blockedPrincipalsIdentifier) ?? Set<String>()
    } catch {
      print(error.localizedDescription)
      return Set<String>()
    }
  }()

  private var cancellables = Set<AnyCancellable>()
  private var isFetchingInitialFeeds = false
  private var feedsBatchSize = 10

  @Published var unifiedState: UnifiedFeedState = .initialized
  @Published var unifiedEvent: UnifiedFeedEvent?

  init(
    fetchFeedsUseCase: FetchInitialFeedsUseCaseProtocol,
    moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol,
    reportUseCase: ReportFeedsUseCaseProtocol,
    logEventUseCase: LogUploadEventUseCaseProtocol,
    socialSignInUseCase: SocialSignInUseCaseProtocol,
    castVoteUseCase: CastVoteUseCaseProtocol
  ) {
    self.initialFeedsUseCase = fetchFeedsUseCase
    self.moreFeedsUseCase = moreFeedsUseCase
    self.reportUseCase = reportUseCase
    self.logEventUseCase = logEventUseCase
    self.socialSignInUseCase = socialSignInUseCase
    self.castVoteUseCase = castVoteUseCase
    self.unifiedEvent = .fetchingInitialFeeds
    isFetchingInitialFeeds = true

    initialFeedsUseCase.feedUpdates
      .receive(on: DispatchQueue.main)
      .sink { [weak self] updatedFeed in
        guard let self = self else { return }
        let unseen = updatedFeed.filter { !self.feedvideoIDSet.contains($0.videoID) }
        guard !unseen.isEmpty else { return }
        self.feedvideoIDSet.formUnion(unseen.map(\.videoID))
        self.filteredFeeds.append(contentsOf: unseen)
        let newUnblocked = unseen.filter { !self.blockedPrincipalIDSet.contains($0.principalID) }
        guard !newUnblocked.isEmpty else { return }
        self.currentFeeds.append(contentsOf: newUnblocked)
        self.unifiedState = .success(feeds: newUnblocked)

      }
      .store(in: &cancellables)
  }

  var unifiedStatePublisher: AnyPublisher<UnifiedFeedState, Never> {
    $unifiedState.eraseToAnyPublisher()
  }

  var unifiedEventPublisher: AnyPublisher<UnifiedFeedEvent?, Never> {
    $unifiedEvent.eraseToAnyPublisher()
  }

  @MainActor func fetchSmileys() async {
    do {
      let result = await SmileyGameConfig.shared.fetch()
      switch result {
      case .success(let success):
        unifiedEvent = .smileysFetched(feeds: currentFeeds)
      case .failure(let failure):
        print(failure.localizedDescription)
      }
    }
  }

  @MainActor func fetchFeeds(request: InitialFeedRequest) async {
    unifiedState = .loading

    do {
      let result = await initialFeedsUseCase.execute(request: request)
      isFetchingInitialFeeds = false
      unifiedEvent = .finishedLoadingInitialFeeds
      if self.currentFeeds.count <= FeedsViewController.Constants.initialNumResults {
        await loadMoreFeeds()
      }
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
      let filteredPosts = filteredFeeds.map {
        FilteredPosts(
          canisterID: $0.canisterID,
          isNsfw: $0.isNsfw,
          postID: $0.postID,
          publisherUserID: $0.principalID,
          videoID: $0.videoID
        )
      }
      let request = MoreFeedsRequest(
        filteredPosts: filteredPosts,
        numResults: feedsBatchSize,
        feedType: .currentUser
      )

      var newFeeds: [FeedResult]?

      let result = await moreFeedsUseCase.execute(request: request)
      switch result {
      case .success(let feeds):
        unifiedEvent = .loadedMoreFeeds
        newFeeds = feeds
      case .failure(let error):
        if case FeedError.aggregated(_, let feeds) = error {
          newFeeds = feeds
        }
        unifiedEvent = .loadMoreFeedsFailed(errorMessage: error.localizedDescription)
        unifiedState = .failure(errorMessage: error.localizedDescription)
      }

      if let feeds = newFeeds, feeds.count < .five {
        feedsBatchSize = min(feedsBatchSize + .ten, FeedsViewController.Constants.maxFeedBatchSize)
        await loadMoreFeeds()
      }
    }
  }

  @MainActor func castVote(request: CastVoteQuery) async {
    do {
      let result = await castVoteUseCase.execute(request: request)
      switch result {
      case .success(let response):
        unifiedEvent = .castVoteSuccess(response)
      case .failure(let error):
        unifiedEvent = .castVoteFailure(error, request.videoID)
      }
    }
  }

  @MainActor func report(request: ReportRequest) async {
    unifiedEvent = .reportInitiated
    let result = await reportUseCase.execute(request: request)
    switch result {
    case .success(let postID):
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: VideoReportedEventData(
          videoId: request.videoId,
          publisherUserId: request.principal,
          isGameEnabled: true,
          gameType: .smiley,
          isNsfw: false,
          reason: request.reason
        )
      )
      unifiedEvent = .reportSuccess(postID)
    case .failure(let failure):
      unifiedEvent = .reportFailed(failure)
      print(failure.localizedDescription)
    }
  }

  @MainActor func blockUser(principalId: String) async {
    blockedPrincipalIDSet.insert(principalId)
    do {
      try KeychainHelper.storeSet(blockedPrincipalIDSet, for: Constants.blockedPrincipalsIdentifier)
    } catch {
      print(error.localizedDescription)
    }
    self.currentFeeds.removeAll(where: { $0.principalID == principalId})
    self.unifiedEvent = .blockedUser(principalId)
  }

  func getCurrentFeedIndex() -> Int {
    return .zero
  }

  func deleteVideo(request: DeleteVideoRequest) async { }

  func log(event: VideoEventRequest) async {
    let result = await logEventUseCase.execute(request: [event])
    switch result {
    case .failure(let failure):
      print(failure.localizedDescription)
    default: break
    }
  }

  func socialSignIn(request: SocialProvider) async {
    let result = await self.socialSignInUseCase.execute(request: request)
    await MainActor.run {
      switch result {
      case .success:
        self.unifiedEvent = .socialSignInSuccess
      case .failure:
        self.unifiedEvent = .socialSignInFailure
      }
    }
  }

  @MainActor func refreshFeeds() async {
    currentFeeds = [FeedResult]()
    filteredFeeds = [FeedResult]()
    feedvideoIDSet = Set<String>()
    blockedPrincipalIDSet = Set<String>()
    do {
      try KeychainHelper.deleteItem(for: Constants.blockedPrincipalsIdentifier)
    } catch {
      print(error)
    }
    self.unifiedEvent = .feedsRefreshed
  }
}

extension FeedsViewModel {
  enum Constants {
    static let blockedPrincipalsIdentifier: String = "blockedPrincipalsIdentifier"
  }
}
