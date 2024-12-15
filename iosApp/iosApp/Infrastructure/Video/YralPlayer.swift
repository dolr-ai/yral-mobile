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
  private var currentPlayerItem: AVPlayerItem?
  private var nextPlayerItem: AVPlayerItem?
  private var feedResults: [FeedResult] = []
  private var currentIndex: Int = 0
  let player = AVPlayer()

  func loadInitialVideos(_ feedResults: [FeedResult]) {
    self.feedResults = feedResults
    currentIndex = 0
    prepareCurrentVideo()
  }

  private func prepareCurrentVideo() {
    guard currentIndex < feedResults.count else { return }

    let currentVideo = feedResults[currentIndex]
    let asset = AVURLAsset(url: currentVideo.url)

    asset.loadValuesAsynchronously(forKeys: ["playable"]) { [weak self] in
      guard let self = self else { return }

      var error: NSError?
      let status = asset.statusOfValue(forKey: "playable", error: &error)
      DispatchQueue.main.async {
        if status == .loaded {
          let item = AVPlayerItem(asset: asset)
          self.currentPlayerItem = item
          self.player.replaceCurrentItem(with: item)
          self.player.play()

          self.preloadNextVideo()
        } else {
          print("Failed to load asset: \(error?.localizedDescription ?? "Unknown error")")
        }
      }
    }
  }

  private func preloadNextVideo() {
    let nextIndex = currentIndex + 1
    guard nextIndex < feedResults.count else {
      nextPlayerItem = nil
      return
    }

    let nextVideo = feedResults[nextIndex]
    let nextAsset = AVURLAsset(url: nextVideo.url)

    nextAsset.loadValuesAsynchronously(forKeys: ["playable"]) { [weak self] in
      guard let self = self else { return }
      var error: NSError?
      let status = nextAsset.statusOfValue(forKey: "playable", error: &error)

      DispatchQueue.main.async {
        if status == .loaded {
          let nextItem = AVPlayerItem(asset: nextAsset)
          self.nextPlayerItem = nextItem
        } else {
          print("Failed to preload next asset: \(error?.localizedDescription ?? "Unknown error")")
        }
      }
    }
  }

  // Call this when the user scrolls to the next video
  func advanceToNextVideo() {
    currentIndex += 1
    if let nextItem = nextPlayerItem {
      currentPlayerItem = nextItem
      player.replaceCurrentItem(with: nextItem)
      player.play()
      // Now preload the next next video
      preloadNextVideo()
    } else {
      // If we don’t have a preloaded item, just prepare it normally
      prepareCurrentVideo()
    }
  }
}
