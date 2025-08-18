//
//  CreateAIVideoViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum CreateAIVideoScreenState {
  case initialized
  case loading
  case success(AIVideoProviderResponse)
  case failure(Error)

  static func == (lhs: CreateAIVideoScreenState, rhs: CreateAIVideoScreenState) -> Bool {
    switch (lhs, rhs) {
    case (.initialized, .initialized),
      (.loading, .loading),
      (.success, .success):
      return true
    case (.failure, .failure):
      return true
    default:
      return false
    }
  }
}

enum CreateAIVideoScreenEvent {
  case updateSelectedProvider(AIVideoProviderResponse)
}

class CreateAIVideoViewModel: ObservableObject {
  let aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol

  @Published var event: CreateAIVideoScreenEvent?
  @Published var state: CreateAIVideoScreenState = .initialized

  var providers: [AIVideoProviderResponse]?
  var selectedProvider: AIVideoProviderResponse?

  init(aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol) {
    self.aiVideoProviderUseCase = aiVideoProviderUseCase
  }

  @MainActor
  func getAIVideoProviders() async {
    state = .loading
    do {
      let result = await aiVideoProviderUseCase.execute()
      switch result {
      case .success(let response):
        self.providers = response.providers
        if let selectedProvider = response.providers.first {
          self.selectedProvider = selectedProvider
          state = .success(selectedProvider)
        }
      case .failure(let error):
        state = .failure(error)
      }
    }
  }

  func updateSelectedProvider(_ provider: AIVideoProviderResponse) {
    self.selectedProvider = provider
    self.event = .updateSelectedProvider(provider)
  }
}
