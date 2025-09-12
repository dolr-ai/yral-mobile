//
//  ProfileViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine
import iosSharedUmbrella

enum ProfilePageState {
  case initialized
  case loading
  case success
  case failure(String)
}

enum ProfilePageEvent: Equatable {
  case fetchedAccountInfo(AccountInfo)
  case loadedVideos([ProfileVideoInfo])
  case pageEndReached(isEmpty: Bool)
  case deletedVideos([ProfileVideoInfo])
  case deleteVideoFailed(String)
  case refreshed([ProfileVideoInfo])
}

class ProfileViewModel: ObservableObject {
  @Published var state: ProfilePageState = .initialized
  @Published var event: ProfilePageEvent?
  var feeds = [FeedResult]()

  let accountUseCase: AccountUseCaseProtocol
  let myVideosUseCase: MyVideosUseCaseProtocol
  let deleteVideoUseCase: DeleteVideoUseCaseProtocol
  let refreshVideoUseCase: RefreshVideosUseCaseProtocol
  private var cancellables = Set<AnyCancellable>()
  var deletedVideos: [ProfileVideoInfo] = []
  var startIndex = Int.zero
  var offset = ProfileRepository.Constants.offset
  private var isLoading = false
  private(set) var hasMorePages = true

  init(
    accountUseCase: AccountUseCaseProtocol,
    myVideosUseCase: MyVideosUseCaseProtocol,
    deleteVideoUseCase: DeleteVideoUseCaseProtocol,
    refreshVideoUseCase: RefreshVideosUseCaseProtocol
  ) {
    self.accountUseCase = accountUseCase
    self.myVideosUseCase = myVideosUseCase
    self.deleteVideoUseCase = deleteVideoUseCase
    self.refreshVideoUseCase = refreshVideoUseCase
    myVideosUseCase.videosPublisher
      .sink { [weak self] videos in
        guard let self = self else { return }
        self.feeds = videos
      }
      .store(in: &cancellables)

    myVideosUseCase.newVideosPublisher
      .map { feedResults in
        feedResults.map { $0.toProfileVideoInfo() }
      }
      .receive(on: RunLoop.main)
      .sink { [weak self] newVideos in
        guard let self = self else { return }
        self.event = .loadedVideos(newVideos)
      }
      .store(in: &cancellables)

    deleteVideoUseCase.deletedVideoPublisher.map { feedResults in
        feedResults.map { $0.toProfileVideoInfo() }
    }
    .receive(on: RunLoop.main)
    .sink { [weak self] deletedVideos in
      guard let self = self, !deletedVideos.isEmpty else { return }
      self.deletedVideos += deletedVideos
      self.event = .deletedVideos(deletedVideos)
    }
    .store(in: &cancellables)
  }

  func fetchProfileInfo() async {
    await MainActor.run {
      state = .loading
    }
    let result = await accountUseCase.execute()
    await MainActor.run {
      switch result {
      case .success(let profileInfo):
        event = .fetchedAccountInfo(profileInfo)
        state = .success
      case .failure(let error):
        state = .failure(error.localizedDescription)
      }
    }
  }

  func getVideos() async {
    guard !isLoading, hasMorePages else { return }

    await MainActor.run {
      state = .loading
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
        state = .success
        startIndex += offset
      case .failure(let error):
        switch error {
        case .pageEndReached:
          event = .pageEndReached(isEmpty: self.feeds.isEmpty)
          state = .success
          hasMorePages = false
        default:
          state = .failure(error.localizedDescription)
        }
      }
      isLoading = false
    }
  }

  func deleteVideo(request: DeleteVideoRequest) async {
    let result = await deleteVideoUseCase.execute(
      request: DeleteVideoRequest(postId: request.postId, videoId: request.videoId)
    )
    await MainActor.run {
      switch result {
      case .success:
        state = .success
      case .failure(let failure):
        event = .deleteVideoFailed(failure.localizedDescription)
        state = .failure(failure.localizedDescription)
      }
    }
  }

  func refreshVideos(request: RefreshVideosRequest) async {
    if request.shouldPurge {
      startIndex = .zero
      offset = ProfileRepository.Constants.offset
    }
    guard !isLoading else { return }

    await MainActor.run {
      state = .loading
      isLoading = true
    }
    let result = await refreshVideoUseCase.execute(request: request)
    await MainActor.run {
      switch result {
      case .success(let videos):
        state = .success
        event = .refreshed(videos.map { $0.toProfileVideoInfo() })
      case .failure(let error):
        event = .refreshed([])
        switch error {
        case .pageEndReached:
          state = .success
          hasMorePages = false
        default:
          state = .failure(error.localizedDescription)
        }
      }
      isLoading = false
    }
  }

  func setAnalyticsInfo(analyticsInfo: AnalyticsInfo) {
    AnalyticsModuleKt.getAnalyticsManager().setUserProperties(
      user: User(
        userId: analyticsInfo.userPrincipal,
        canisterId: analyticsInfo.canisterPrincipal,
        isLoggedIn: KotlinBoolean(bool: analyticsInfo.isLoggedIn),
        isCreator: KotlinBoolean(bool: self.feeds.count >= .one),
        walletBalance: KotlinDouble(value: analyticsInfo.walletBalance),
        tokenType: .yral,
        isForcedGamePlayUser: KotlinBoolean(
          bool: AppDIHelper().getFeatureFlagManager().isEnabled(
            flag: FeedFeatureFlags.SmileyGame.shared.StopAndVoteNudge
          )
        ),
        emailId: analyticsInfo.emailId
      )
    )
    AnalyticsModuleKt.getAnalyticsManager().flush()
  }

  struct AnalyticsInfo {
    let userPrincipal: String
    let canisterPrincipal: String
    let isLoggedIn: Bool
    let walletBalance: Double
    let playToScroll: Bool
    let emailId: String?
  }
}
