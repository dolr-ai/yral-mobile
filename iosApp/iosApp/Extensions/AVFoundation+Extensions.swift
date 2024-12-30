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
