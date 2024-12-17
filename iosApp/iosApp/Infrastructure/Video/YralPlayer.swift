//
//  YralPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

//
//  YralPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

class YralPlayer {
  private var feedResults: [FeedResult] = []
  var currentIndex: Int = 0

  // Using AVQueuePlayer for looping capabilities
  let player = AVQueuePlayer()

  // Keep a reference to the AVPlayerLooper to prevent it from being deallocated
  private var playerLooper: AVPlayerLooper?

  private var playerItems: [Int: AVPlayerItem] = [:]

  func loadInitialVideos(_ feedResults: [FeedResult]) {
    self.feedResults = feedResults
    player.isMuted = false
    currentIndex = 0
    prepareCurrentVideo()
  }

  func advanceToVideo(at index: Int) {
    guard index >= 0 && index < feedResults.count else { return }
    currentIndex = index

    if let preloadedItem = playerItems[index] {
      // Use the preloaded item
      startLooping(with: preloadedItem)
    } else {
      // Need to load it asynchronously
      prepareCurrentVideo()
    }
  }

  private func prepareCurrentVideo() {
    guard currentIndex < feedResults.count else { return }
    loadVideo(at: currentIndex) { [weak self] item in
      guard let self = self, let item = item else { return }
      self.startLooping(with: item)
    }
  }

  private func startLooping(with item: AVPlayerItem) {
    // Reset the player to ensure no previous items remain
    player.removeAllItems()
    player.insert(item, after: nil)

    // Create a new looper for the current item
    // AVPlayerLooper needs a template item and will handle looping internally
    playerLooper = AVPlayerLooper(player: player, templateItem: item)
    player.play()
  }

  private func preloadAdjacentVideos() {
    let nextIndex = currentIndex + 1
    let prevIndex = currentIndex - 1

    if nextIndex < feedResults.count && playerItems[nextIndex] == nil {
      loadVideo(at: nextIndex, completion: nil)
    }

    if prevIndex >= 0 && playerItems[prevIndex] == nil {
      loadVideo(at: prevIndex, completion: nil)
    }
  }

  private func loadVideo(at index: Int, completion: ((AVPlayerItem?) -> Void)?) {
    let video = feedResults[index]
    let asset = AVURLAsset(url: video.url)
    asset.loadValuesAsynchronously(forKeys: ["playable"]) { [weak self] in
      guard let self = self else { return }

      var error: NSError?
      let status = asset.statusOfValue(forKey: "playable", error: &error)

      DispatchQueue.main.async {
        if status == .loaded {
          let item = AVPlayerItem(asset: asset)
          self.playerItems[index] = item
          completion?(item)
        } else {
          print("Failed to load asset: \(error?.localizedDescription ?? "Unknown error")")
          completion?(nil)
        }
      }
    }
  }
}
