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
  case loadedMoreFeeds([FeedResult])
}

class FeedsViewModel: ObservableObject {
  @Published var state: FeedsPageState = .initalized
  @Published var event: FeedsPageEvent?

  var feedsUseCase: FeedsUseCase
  private var currentFeeds = [FeedResult]()
  init(useCase: FeedsUseCase) {
    self.feedsUseCase = useCase
  }

  func fetchFeeds(request: FeedRequest) async {
    state = .loading
    do {
      let result = try await feedsUseCase.execute(request: request)
      switch result {
      case .success(let response):
        currentFeeds = response
        state = .successfullyFetched(currentFeeds)
      case .failure(let error):
        state = .failure(error)
      }
    } catch {
      state = .failure(error)
    }
  }

  func loadMoreFeeds() async {
    state = .loading
    do {
      let filteredPosts = currentFeeds.map { feed in
        var item = MlFeed_PostItem()
        item.canisterID = feed.canisterID
        item.postID = UInt32(feed.postID) ?? .zero
        item.videoID = feed.videoID
        return item
      }
      let request = FeedRequest(
        filteredPosts: filteredPosts,
        numResults: FeedsViewController.Constants.initialNumResults
      )
      let result = try await feedsUseCase.execute(request: request)
      switch result {
      case .success(let response):
        event = .loadedMoreFeeds(response)
        currentFeeds += response
        state = .successfullyFetched(currentFeeds)
      case .failure(let error):
        state = .failure(error)
      }
    } catch {
      state = .failure(error)
    }
  }
}
