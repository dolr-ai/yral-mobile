//
//  UploadViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum UploadViewState {
  case initialized
  case loading
  case success
  case failure(Error)

  static func == (lhs: UploadViewState, rhs: UploadViewState) -> Bool {
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

enum UploadViewEvent {
  case videoSelected(URL)
  case uploadProgressUpdated(Double)
  case uploadPressed
  case videoUploadSuccess
  case videoUploadCancelled
  case videoUploadFailure(Error)
}

class UploadViewModel: ObservableObject {
  let getUploadEndpointUseCase: GetUploadEndpointUseCase
  let uploadVideoUseCase: UploadVideoUseCase
  let updateMetaUseCase: UpdateMetaUseCase
  var uploadEndpointResponse: UploadEndpointResponse!
  private var fetchEndpointTask: Task<Void, Never>?

  @Published var event: UploadViewEvent?
  @Published var state: UploadViewState = .initialized

  private var uploadTask: Task<Void, Never>?

  init(getUploadEndpointUseCase: GetUploadEndpointUseCase,
       uploadVideoUseCase: UploadVideoUseCase,
       updateMetaUseCase: UpdateMetaUseCase) {
    self.getUploadEndpointUseCase = getUploadEndpointUseCase
    self.uploadVideoUseCase = uploadVideoUseCase
    self.updateMetaUseCase = updateMetaUseCase
  }

  func getUploadEndpoint() async {
    guard fetchEndpointTask == nil else { return }
    fetchEndpointTask = Task {
      let result = await getUploadEndpointUseCase.execute()
      await MainActor.run {
        switch result {
        case .success(let success):
          self.uploadEndpointResponse = success
        case .failure(let failure):
          print("Failed to fetch upload endpoint: \(failure)")
        }
        self.fetchEndpointTask = nil
      }
    }
  }

  func handleVideoPicked(_ url: URL) {
    event = .videoSelected(url)
  }

  func startUpload(fileURL: URL, caption: String, hashtags: [String]) {
    uploadTask?.cancel()
    uploadTask = Task {
      if let fetchTask = self.fetchEndpointTask {
        await fetchTask.value
      }
      guard let uploadURLString = uploadEndpointResponse?.url else {
        await MainActor.run {
          state = .failure(VideoUploadError.invalidUploadURL("No valid upload URL found."))
        }
        return
      }

      await MainActor.run {
        state = .loading
      }

      let progressStream = uploadVideoUseCase.execute(
        request: UploadVideoRequest(
          fileURL: fileURL,
          videoUID: uploadEndpointResponse.videoID,
          uploadURLString: uploadURLString,
          caption: caption,
          hashtags: hashtags
        )
      )

      do {
        for try await progress in progressStream {
          await MainActor.run {
            self.event = .uploadProgressUpdated(progress)
          }
        }
      } catch {
        let finalError: VideoUploadError
        if let uploadErr = error as? VideoUploadError {
          finalError = uploadErr
        } else {
          finalError = .unknown(error)
        }
        await MainActor.run {
          self.state = .failure(finalError)
        }
      }
    }
  }

  func cancelUpload() {
    uploadTask?.cancel()
    event = .videoUploadCancelled
  }

  func finishUpload(fileURL: URL, caption: String, hashtags: [String]) async {
    guard let uploadURLString = uploadEndpointResponse?.url else {
      await MainActor.run {
        state = .failure(VideoUploadError.invalidUploadURL("No valid upload URL found."))
      }
      return
    }
    await MainActor.run {
      state = .loading
    }
    let result = await updateMetaUseCase.execute(
      request: UploadVideoRequest(
        fileURL: fileURL,
        videoUID: uploadEndpointResponse.videoID,
        uploadURLString: uploadURLString,
        caption: caption,
        hashtags: hashtags
      )
    )
    await MainActor.run {
      self.uploadEndpointResponse = nil
      switch result {
      case .success:
        switch state {
        case .failure(let error):
          self.event = .videoUploadFailure(error)
        default:
          self.state = .success
          self.event = .videoUploadSuccess
        }
      case .failure(let failure):
        self.state = .failure(failure)
        self.event = .videoUploadFailure(failure)
      }
    }
  }
}
