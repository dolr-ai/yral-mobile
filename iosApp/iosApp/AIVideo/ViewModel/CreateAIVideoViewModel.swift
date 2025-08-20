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
  case socialSignInSuccess(creditsAvailable: Bool)
  case socialSignInFailure
  case generateVideoSuccess(GenerateVideoResponse)
  case generateVideoFailure(String)
}

class CreateAIVideoViewModel: ObservableObject {
  let aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol
  let rateLimitStatusUseCase: RateLimitStatusUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol
  let generateVideoUseCase: GenerateVideoUseCaseProtocol

  @Published var event: CreateAIVideoScreenEvent?
  @Published var state: CreateAIVideoScreenState = .initialized

  var providers: [AIVideoProviderResponse]?
  var selectedProvider: AIVideoProviderResponse?

  init(
    aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol,
    rateLimitStatusUseCase: RateLimitStatusUseCaseProtocol,
    socialSigninUseCase: SocialSignInUseCaseProtocol,
    generateVideoUseCase: GenerateVideoUseCase
  ) {
    self.aiVideoProviderUseCase = aiVideoProviderUseCase
    self.rateLimitStatusUseCase = rateLimitStatusUseCase
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
    switch result {
    case .success:
      let creditsAvailable = await creditsAvailable()
      await MainActor.run {
        self.event = .socialSignInSuccess(creditsAvailable: creditsAvailable)
      }
    case .failure:
      await MainActor.run {
        self.event = .socialSignInFailure
      }
    }
  }

  func creditsAvailable() async -> Bool {
    do {
      let result = await rateLimitStatusUseCase.execute()
      switch result {
      case .success(let status):
        return !status.is_limited()
      case .failure:
        return false
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
