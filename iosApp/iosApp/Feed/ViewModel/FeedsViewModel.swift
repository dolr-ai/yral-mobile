//
//  FeedsViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

enum FeedsPageState {
  case initalized
  case loading
  case successfullyFetched([FeedResult])
  case failure(Error)
}

enum FeedsPageEvent {
  case reachedEnd
}

class FeedsViewModel: ObservableObject {
  @Published var state: FeedsPageState = .initalized
  var feedsUseCase: FeedsUseCase

  init(useCase: FeedsUseCase) {
    self.feedsUseCase = useCase
  }

  func fetchFeeds(request: FeedRequest) async {
    state = .loading
    do {
      let result = try await feedsUseCase.execute(request: request)
      switch result {
      case .success(let response):
        state = .successfullyFetched(response)
      case .failure(let error):
        state = .failure(error)
      }
    } catch {
      state = .failure(error)
    }
  }
}
