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
  let reportUseCase: ReportFeedsUseCaseProtocol
  let logEventUseCase: LogUploadEventUseCaseProtocol
  let smileyConfigUseCase: SmileyUseCaseProtocol

  private var smileys = [Smiley]()
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

  @Published var unifiedState: UnifiedFeedState = .initialized
  @Published var unifiedEvent: UnifiedFeedEvent?

  init(
    fetchFeedsUseCase: FetchInitialFeedsUseCaseProtocol,
    moreFeedsUseCase: FetchMoreFeedsUseCaseProtocol,
    reportUseCase: ReportFeedsUseCaseProtocol,
    logEventUseCase: LogUploadEventUseCaseProtocol,
    smileyConfigUseCase: SmileyUseCaseProtocol
  ) {
    self.initialFeedsUseCase = fetchFeedsUseCase
    self.moreFeedsUseCase = moreFeedsUseCase
    self.reportUseCase = reportUseCase
    self.logEventUseCase = logEventUseCase
    self.smileyConfigUseCase = smileyConfigUseCase
    self.unifiedEvent = .fetchingInitialFeeds
    isFetchingInitialFeeds = true

    initialFeedsUseCase.feedUpdates
      .receive(on: DispatchQueue.main)
      .sink { [weak self] updatedFeed in
        guard let self = self else { return }
        self.filteredFeeds = updatedFeed.filter {
          !self.feedvideoIDSet.contains($0.videoID)
        }
        let unblockedFeeds = self.filteredFeeds.filter { !self.blockedPrincipalIDSet.contains($0.principalID) }
        guard !unblockedFeeds.isEmpty else { return }
        self.feedvideoIDSet.formUnion(unblockedFeeds.map { $0.videoID })
        let modifiedUnblockedFeeds = unblockedFeeds.map { item in
          var modified = item
          modified.smileyGame = SmileyGame(smileys: self.smileys, result: nil)
          return modified
        }
        self.currentFeeds += modifiedUnblockedFeeds
        self.unifiedState = .success(feeds: modifiedUnblockedFeeds)
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
    unifiedState = .loading
    do {
      let result = await smileyConfigUseCase.execute()
      switch result {
      case .success(let smileyConfig):
        smileys = smileyConfig.smileys
      case .failure(let error):
        print(error.localizedDescription)
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
      case .success:
        unifiedEvent = .loadedMoreFeeds
      case .failure(let error):
        unifiedEvent = .loadMoreFeedsFailed(errorMessage: error.localizedDescription)
        unifiedState = .failure(errorMessage: error.localizedDescription)
      }
      if self.currentFeeds.count <= FeedsViewController.Constants.initialNumResults {
        await loadMoreFeeds()
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
}

extension FeedsViewModel {
  enum Constants {
    static let blockedPrincipalsIdentifier: String = "blockedPrincipalsIdentifier"
  }
}
