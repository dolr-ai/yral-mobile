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
  case success
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
  case socialSignInSuccess
  case socialSignInFailure
  case generateVideoSuccess(GenerateVideoResponse)
  case generateVideoFailure(String)
}

class CreateAIVideoViewModel: ObservableObject {
  let aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol
  let generateVideoUseCase: GenerateVideoUseCaseProtocol

  @Published var event: CreateAIVideoScreenEvent?
  @Published var state: CreateAIVideoScreenState = .initialized

  var providers: [AIVideoProviderResponse]?
  var selectedProvider: AIVideoProviderResponse?

  init(
    aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol,
    socialSigninUseCase: SocialSignInUseCaseProtocol,
    generateVideoUseCase: GenerateVideoUseCase
  ) {
    self.aiVideoProviderUseCase = aiVideoProviderUseCase
    self.socialSignInUseCase = socialSigninUseCase
    self.generateVideoUseCase = generateVideoUseCase
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
          state = .success
          event = .updateSelectedProvider(selectedProvider)
        }
      case .failure(let error):
        state = .failure(error)
      }
    }
  }

  func socialSignIn(request: SocialProvider) async {
    let result = await self.socialSignInUseCase.execute(request: request)
    await MainActor.run {
      switch result {
      case .success:
        self.event = .socialSignInSuccess
      case .failure:
        self.event = .socialSignInFailure
      }
    }
  }

  @MainActor
  func generateVideo(for prompt: String, withProvider provider: AIVideoProviderResponse) async {
    state = .loading

    let generateVideoRequest = GenerateVideoMetaRequest(
      request: GenerateVideoRequest(
        aspectRatio: "16:9",
        durationSeconds: provider.defaultDuration,
        generateAudio: true,
        image: nil,
        modelID: provider.id,
        negativePrompt: nil,
        prompt: prompt,
        resolution: nil,
        seed: nil,
        tokenType: "Free",
        userID: nil
      )
    )

    do {
      let result = await generateVideoUseCase.execute(request: generateVideoRequest)
      switch result {
      case .success(let response):
        state = .success
        event = .generateVideoSuccess(response)
      case .failure(let error):
        state = .failure(error)
        event = .generateVideoFailure(error.localizedDescription)
      }
    }
  }

  func updateSelectedProvider(_ provider: AIVideoProviderResponse) {
    self.selectedProvider = provider
    self.event = .updateSelectedProvider(provider)
  }
}
