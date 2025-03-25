//
//  ProfileViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine

enum ProfilePageState {
  case initialized
  case loading
  case success
  case failure(String)
}

enum ProfilePageEvent {
  case fetchedAccountInfo(AccountInfo)
  case loadedVideos([ProfileVideoInfo])
  case pageEndReached
  case deletedVideos([ProfileVideoInfo])
  case deleteVideoFailed(String)
}

class ProfileViewModel: ObservableObject {
  @Published var state: ProfilePageState = .initialized
  @Published var event: ProfilePageEvent?
  var feeds = [FeedResult]()

  let accountUseCase: AccountUseCaseProtocol
  let myVideosUseCase: MyVideosUseCaseProtocol
  let deleteVideoUseCase: DeleteVideoUseCaseProtocol
  private var cancellables = Set<AnyCancellable>()
  var startIndex = Int.zero
  var offset = ProfileRepository.Constants.offset
  private var isLoading = false
  private(set) var hasMorePages = true

  init(
    accountUseCase: AccountUseCaseProtocol,
    myVideosUseCase: MyVideosUseCaseProtocol,
    deleteVideoUseCase: DeleteVideoUseCaseProtocol
  ) {
    self.accountUseCase = accountUseCase
    self.myVideosUseCase = myVideosUseCase
    self.deleteVideoUseCase = deleteVideoUseCase
    myVideosUseCase.videosPublisher
      .receive(on: RunLoop.main)
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
      guard let self = self else { return }
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
          event = .pageEndReached
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
      case .success(let success):
        state = .success
      case .failure(let failure):
        event = .deleteVideoFailed(failure.localizedDescription)
        state = .failure(failure.localizedDescription)
      }
    }
  }
}
