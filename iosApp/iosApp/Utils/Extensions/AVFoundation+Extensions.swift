//
//  AVFoundation+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import AVFoundation

extension AVURLAsset {
  func loadPlayableAsync() async throws {
    try await withCheckedThrowingContinuation { continuation in
      self.loadValuesAsynchronously(forKeys: ["playable"]) {
        var error: NSError?
        let status = self.statusOfValue(forKey: "playable", error: &error)
        if status == .loaded {
          continuation.resume()
        } else {
          let msg = error?.localizedDescription ?? "Asset not playable"
          let err = NSError(domain: "AVURLAsset.loadPlayableAsync",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: msg])
          continuation.resume(throwing: err)
        }
      }
    }
  }
}

extension AVPlayerItem {
  @MainActor
  func waitUntilReady() async throws {
    if status == .readyToPlay { return }

    try await withCheckedThrowingContinuation { continuation in
      var token: NSKeyValueObservation?
      token = observe(\.status, options: [.initial, .new]) { item, _ in
        switch item.status {
        case .readyToPlay:
          token?.invalidate()
          continuation.resume()
        case .failed:
          token?.invalidate()
          continuation.resume(throwing: item.error ?? AVError(.unknown))
        case .unknown:
          break
        @unknown default:
          break
        }
      }
    }
  }
}

extension AVQueuePlayer {
  @MainActor
  func waitForFirstItem() async -> AVPlayerItem {
    if let item = currentItem { return item }
    return await withCheckedContinuation { continuation in
      var observation: NSKeyValueObservation?
      observation = observe(\.currentItem, options: [.new]) { _, change in
        if let newItem = change.newValue as? AVPlayerItem {
          observation?.invalidate()
          continuation.resume(returning: newItem)
        }
      }
    }
  }
}
