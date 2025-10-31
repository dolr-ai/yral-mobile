//
//  VideoInsightsViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 30/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum VideoInsightsPageState {
  case initialized
  case loading
  case success
  case failure
}

enum VideoInsightsPageEvent: Equatable {
  case fetchedVideoInsights([VideoInsightsDTO])
  case failedToFetchVideoInsights(String)
}

class VideoInsightsViewModel: ObservableObject {
  @Published var state: VideoInsightsPageState = .initialized
  @Published var event: VideoInsightsPageEvent?

  let videoInsightsUseCase: VideoInsightsUseCaseProtocol

  init(videoInsightsUseCase: VideoInsightsUseCaseProtocol) {
    self.videoInsightsUseCase = videoInsightsUseCase
  }

  func fetchVideoInsights(videoIDs: [String]) async {
    await MainActor.run {
      state = .loading
    }

    let result = await videoInsightsUseCase.execute(request: VideoInsightsRequestDTO(videoIDs: videoIDs))
    await MainActor.run {
      switch result {
      case .success(let videosInsights):
        event = .fetchedVideoInsights(videosInsights)
        state = .success
      case .failure(let error):
        event = .failedToFetchVideoInsights(error.localizedDescription)
        state = .failure
      }
    }
  }
}
