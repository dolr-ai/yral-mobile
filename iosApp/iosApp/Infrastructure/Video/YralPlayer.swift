//
//  YralPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

<<<<<<< HEAD
@MainActor
final class YralPlayer {
  private var feedResults: [FeedResult] = []
  var currentIndex: Int = .zero

  let player = AVQueuePlayer()
  private var playerLooper: AVPlayerLooper?
  private var lastPlayedTimes: [Int: CMTime] = [:]
  private var playerItems: [Int: AVPlayerItem] = [:]
  private var currentlyDownloadingIndexes: Set<Int> = []

  func loadInitialVideos(_ feedResults: [FeedResult]) {
    self.feedResults = feedResults
    configureAudioSession()
    currentIndex = .zero
    HLSDownloadManager.shared.delegate = self
    Task {
      await prepareCurrentVideo()
    }
  }

  private func configureAudioSession() {
    do {
      player.isMuted = false
      try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
      try AVAudioSession.sharedInstance().setActive(true)
    } catch {
      print("Failed to configure AVAudioSession: \(error)")
    }
  }

  func addFeedResults(_ feedResults: [FeedResult]) {
    self.feedResults += feedResults
    if self.feedResults.count <= Constants.radius {
      Task {
        await preloadFeeds()
      }
    }
  }

  func advanceToVideo(at index: Int) {
    guard index >= 0 && index < feedResults.count else { return }
    if let currentTime = player.currentItem?.currentTime() {
      lastPlayedTimes[currentIndex] = currentTime
    }
    if abs(index - currentIndex) > Constants.radius {
      cancelPreloadOutsideRange(center: index, radius: Constants.radius)
    }

    currentIndex = index
    Task {
      if let preloadedItem = playerItems[index] {
        startLooping(with: preloadedItem)
      } else {
        await prepareCurrentVideo()
      }
    }
  }

  private func prepareCurrentVideo() async {
    guard currentIndex < feedResults.count else { return }
    do {
      if let item = try await loadVideo(at: currentIndex) {
        startLooping(with: item)
      }
    } catch {
      print("Error loading current video at index \(currentIndex): \(error)")
    }
  }

  private func startLooping(with item: AVPlayerItem) {
    player.removeAllItems()
    playerLooper = AVPlayerLooper(player: player, templateItem: item)

    if let lastTime = lastPlayedTimes[currentIndex] {
      player.seek(to: lastTime, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
        Task { @MainActor [weak self] in
          guard let self = self else { return }
          self.play()
        }
      }
    } else {
      play()
    }

    Task {
      await preloadFeeds()
    }
  }

  func play() {
    player.play()
  }

  func pause() {
    player.pause()
  }

  private func preloadFeeds() async {
    let startIndex = currentIndex
    let endIndex = min(feedResults.count, currentIndex + .one + Constants.radius)

    for index in startIndex..<endIndex {
      guard playerItems[index] == nil else { continue }
      guard !currentlyDownloadingIndexes.contains(index) else { continue }
      await downloadVideo(at: index)
    }
  }

  private func downloadVideo(at index: Int) async {
    currentlyDownloadingIndexes.insert(index)
    let feed = feedResults[index]
    let assetTitle = "\(feed.videoID)"

    do {
      _ = try await HLSDownloadManager.shared.startDownloadAsync(
        hlsURL: feed.url,
        assetTitle: assetTitle
      )
      if let item = try await loadVideo(at: index) {
        playerItems[index] = item
      }
    } catch is CancellationError {
      print("Preload canceled for index \(index).")
    } catch {
      print("Preload failed for index \(index): \(error)")
    }

    currentlyDownloadingIndexes.remove(index)
  }

  private func cancelPreloadOutsideRange(center: Int, radius: Int) {
    let validRange = (center - radius)...(center + radius)
    let indicesToCancel = currentlyDownloadingIndexes.filter { !validRange.contains($0) }

    for idx in indicesToCancel {
      let feed = feedResults[idx]
      HLSDownloadManager.shared.cancelDownload(for: feed.url)
      HLSDownloadManager.shared.clearMappingsAndCache(for: feed.url, assetTitle: feed.videoID)
      currentlyDownloadingIndexes.remove(idx)
    }
  }

  private func loadVideo(at index: Int) async throws -> AVPlayerItem? {
    let feed = feedResults[index]
    if let localAsset = HLSDownloadManager.shared.createLocalAssetIfAvailable(for: feed.url) {
      do {
        try await localAsset.loadPlayableAsync()
        let item = AVPlayerItem(asset: localAsset)
        playerItems[index] = item
        return item
      } catch {
        print("Local asset not playable (fallback to remote). Error: \(error)")
      }
    }
    let remoteAsset = AVURLAsset(url: feed.url)
    try await remoteAsset.loadPlayableAsync()
    let item = AVPlayerItem(asset: remoteAsset)
    return item
  }
}

extension YralPlayer: HLSDownloadManagerProtocol {
  nonisolated func clearedCache(for assetTitle: String) {
    Task { @MainActor [weak self] in
      guard let self else { return }
      guard let index = feedResults.firstIndex(where: { $0.videoID == assetTitle }) else { return }
      self.playerItems.removeValue(forKey: index)
    }
  }
}

extension YralPlayer {
  enum Constants {
    static let radius = 5
  }
}
=======
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
        self?.player.play()
      }
    } else {
      player.play()
    }
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
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
