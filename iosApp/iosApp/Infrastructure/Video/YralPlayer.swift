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

  // Store the last played times for each video
  private var lastPlayedTimes: [Int: CMTime] = [:]

  private var playerItems: [Int: AVPlayerItem] = [:]

  func loadInitialVideos(_ feedResults: [FeedResult]) {
    self.feedResults = feedResults
    player.isMuted = false
    currentIndex = 0
    prepareCurrentVideo()
  }

  func addFeedResults(_ feedResults: [FeedResult]) {
    self.feedResults += feedResults
  }
  func advanceToVideo(at index: Int) {
    guard index >= 0 && index < feedResults.count else { return }

    // Save current player's last played time before switching
    if let currentTime = player.currentItem?.currentTime() {
      lastPlayedTimes[currentIndex] = currentTime
    }

    currentIndex = index
    if let preloadedItem = playerItems[index] {
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
    playerLooper = AVPlayerLooper(player: player, templateItem: item)

    // If we have a saved last played time, seek to it
    if let lastTime = lastPlayedTimes[currentIndex] {
      player.seek(to: lastTime, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
        self?.play()
      }
    } else {
      play()
    }
  }

  func play() {
    player.play()
  }

  func pause() {
    player.pause()
  }

  private func preloadFeed() {
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
