//
//  ProfileViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Combine

enum ProfilePageState {
  case initialized
  case loading
  case success
  case failure(String)
}

enum ProfilePageEvent {
  case fetchedAccountInfo(AccountInfo)
  case loadedVideos([FeedResult])
  case pageEndReached
}

class ProfileViewModel: ObservableObject {
  @Published var state: ProfilePageState = .initialized
  @Published var event: ProfilePageEvent?

  let accountUseCase: AccountUseCase
  let myVideosUseCase: MyVideosUseCase
  var videos: [FeedResult] = []
  var startIndex = Int.zero
  var offset = Constants.offset

  init(
    accountUseCase: AccountUseCase,
    myVideosUseCase: MyVideosUseCase
  ) {
    self.accountUseCase = accountUseCase
    self.myVideosUseCase = myVideosUseCase
  }

  func fetchProfileInfo() async {
    state = .loading
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
    state = .loading
    startIndex += offset
    let result = await myVideosUseCase.execute(
      request: AccountVideoRequest(
        startIndex: UInt64(startIndex),
        offset: UInt64(offset)
      )
    )
    await MainActor.run {
      switch result {
      case .success(let feedResult):
        videos += feedResult
        event = .loadedVideos(videos)
        state = .success
      case .failure(let error):
        switch error {
        case .pageEndReached:
          event = .pageEndReached
          state = .success
        default:
          state = .failure(error.localizedDescription)
        }
      }
    }
  }
}

extension ProfileViewModel {
  enum Constants {
    static let offset = 10
  }
}
