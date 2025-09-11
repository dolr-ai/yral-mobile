//
//  CreateAIVideoViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import iosSharedUmbrella

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
  case generateVideoSuccess(deductBalance: Int)
  case generateVideoFailure(String)
  case generateVideoStatusFailure(error: String, addBalance: Int)
  case uploadAIVideoSuccess(String)
  case uploadAIVideoFailure(String)
}

class CreateAIVideoViewModel: ObservableObject {
  let aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol
  let rateLimitStatusUseCase: RateLimitStatusUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol
  let generateVideoUseCase: GenerateVideoUseCaseProtocol
  let generateVideoStatusUseCase: GenerateVideoStatusUseCaseProtocol
  let uploadAIVideoUseCase: UploadAIVideoUseCaseProtocol

  @Published var event: CreateAIVideoScreenEvent?
  @Published var state: CreateAIVideoScreenState = .initialized

  var providers: [AIVideoProviderResponse]?
  var selectedProvider: AIVideoProviderResponse?
  var pollingRequestKey: GenerateVideoRequestKeyResponse?
  var videoURLString: String?

  private var pollingTask: Task<Void, Never>?

  init(
    aiVideoProviderUseCase: AIVideoProviderUseCaseProtocol,
    rateLimitStatusUseCase: RateLimitStatusUseCaseProtocol,
    socialSigninUseCase: SocialSignInUseCaseProtocol,
    generateVideoUseCase: GenerateVideoUseCaseProtocol,
    generateVideoStatusUseCase: GenerateVideoStatusUseCaseProtocol,
    uploadAIVideoUseCase: UploadAIVideoUseCaseProtocol
  ) {
    self.aiVideoProviderUseCase = aiVideoProviderUseCase
    self.rateLimitStatusUseCase = rateLimitStatusUseCase
    self.socialSignInUseCase = socialSigninUseCase
    self.generateVideoUseCase = generateVideoUseCase
    self.generateVideoStatusUseCase = generateVideoStatusUseCase
    self.uploadAIVideoUseCase = uploadAIVideoUseCase
  }

  deinit {
    stopPolling()
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
  func generateVideo(for prompt: String, withProvider provider: AIVideoProviderResponse, usingCredits: Bool) async {
    state = .loading

    let generateVideoRequest = GenerateVideoMetaRequest(
      request: GenerateVideoRequest(
        aspectRatio: provider.defaultAspectRatio,
        durationSeconds: provider.defaultDuration,
        generateAudio: provider.supportsAudio ? true : nil,
        image: nil,
        modelID: provider.id,
        negativePrompt: nil,
        prompt: prompt,
        resolution: nil,
        seed: nil,
        tokenType: usingCredits ? "Free" : "Sats",
        userID: nil
      )
    )

    do {
      let result = await generateVideoUseCase.execute(request: generateVideoRequest)
      switch result {
      case .success(let response):
        pollingRequestKey = response.requestKey
        state = .success
        event = .generateVideoSuccess(deductBalance: provider.cost.sats)
      case .failure(let error):
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: AiVideoGeneratedData(
            model: selectedProvider?.name ?? "",
            isSuccess: false,
            reason: error.localizedDescription,
            reasonType: .triggerFailed
          )
        )

        state = .failure(error)
        event = .generateVideoFailure(error.localizedDescription)
      }
    }
  }

  func getGenerateVideoStatus(for request: GenerateVideoRequestKeyResponse) async {
    do {
      let result = await generateVideoStatusUseCase.execute(request: UInt64(request.counter))
      await MainActor.run {
        switch result {
        case .success(let status):
          if status.contains("Complete: ") {
            stopPolling()
            videoURLString = status.replacingOccurrences(of: "Complete: ", with: "")
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: AiVideoGeneratedData(
                model: selectedProvider?.name ?? "",
                isSuccess: true,
                reason: nil,
                reasonType: nil
              )
            )
            Task { await uploadAIVideo() }
          } else if status.contains("Failed: ") {
            stopPolling()
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: AiVideoGeneratedData(
                model: selectedProvider?.name ?? "",
                isSuccess: false,
                reason: "Video generation failed",
                reasonType: .generationFailed
              )
            )
            event = .generateVideoStatusFailure(
              error: status.replacingOccurrences(of: "Failed: ", with: ""),
              addBalance: selectedProvider?.cost.sats ?? .zero
            )
          }
        case .failure(let error):
          stopPolling()
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: AiVideoGeneratedData(
              model: selectedProvider?.name ?? "",
              isSuccess: false,
              reason: error.localizedDescription,
              reasonType: .generationFailed
            )
          )
          state = .failure(error)
          event = .generateVideoStatusFailure(
            error: error.localizedDescription,
            addBalance: selectedProvider?.cost.sats ?? .zero
          )
        }
      }
    }
  }

  func uploadAIVideo() async {
    guard let videoURL = videoURLString else {
      event = .uploadAIVideoFailure("No URL was found to upload the video")
      return
    }

    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoUploadInitiatedEventData(captionAdded: false, hashtagsAdded: false, type: .aiVideo)
    )

    do {
      let result = await uploadAIVideoUseCase.execute(request: videoURL)
      await MainActor.run {
        switch result {
        case .success:
          event = .uploadAIVideoSuccess(videoURL)
        case .failure(let error):
          state = .failure(error)
          event = .uploadAIVideoFailure(error.localizedDescription)
        }
      }
    }
  }

  func startPolling() {
    if let task = pollingTask, !task.isCancelled {
      return
    }

    guard let request = pollingRequestKey else {
      event = .generateVideoStatusFailure(
        error: "No request key was found to generate video",
        addBalance: selectedProvider?.cost.sats ?? .zero
      )
      return
    }

    pollingTask = Task { [weak self] in
      while !Task.isCancelled {
        guard let self else { return }
        await self.getGenerateVideoStatus(for: request)
        try? await Task.sleep(nanoseconds: 5 * 1_000_000_000)
      }
    }
  }

  func stopPolling() {
    pollingTask?.cancel()
    pollingTask = nil
    pollingRequestKey = nil
  }

  func updateSelectedProvider(_ provider: AIVideoProviderResponse) {
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoGenerationModelSelectedData(model: provider.name)
    )

    self.selectedProvider = provider
    self.event = .updateSelectedProvider(provider)
  }
}
