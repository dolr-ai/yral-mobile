//
//  FeedsPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

@MainActor
final class FeedsPlayer: YralPlayer {
  var feedResults: [FeedResult] = []
  var currentIndex: Int = .zero

  let player = AVQueuePlayer()
  private var playerLooper: AVPlayerLooper?
  private var lastPlayedTimes: [Int: CMTime] = [:]
  var playerItems: [Int: AVPlayerItem] = [:]
  private var currentlyDownloadingIndexes: Set<Int> = []
  var isPlayerVisible: Bool = true
  var didEmptyFeeds: (() -> Void)?
  weak var delegate: FeedsPlayerProtocol?

  func loadInitialVideos(_ feeds: [FeedResult]) {
    self.feedResults = feeds
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

  func addFeedResults(_ feeds: [FeedResult]) {
    self.feedResults += feeds
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

  func removeFeeds(_ feeds: [FeedResult]) {
    guard !feeds.isEmpty else { return }
    let removedIDs = Set(feeds.map { $0.postID })
    let currentFeedID = feedResults.indices.contains(currentIndex)
    ? feedResults[currentIndex].postID
    : nil
    feedResults.removeAll(where: { removedIDs.contains($0.postID) })
    playerItems.removeAll()
    if let currentFeedID = currentFeedID,
       !feedResults.contains(where: { $0.postID == currentFeedID }) {
      currentIndex = min(currentIndex, max(feedResults.count - 1, .zero))
      advanceToVideo(at: currentIndex)
    }
    if feedResults.isEmpty {
      didEmptyFeeds?()
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
    playerLooper?.disableLooping()
    playerLooper = nil
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
    if isPlayerVisible == true {
      player.play()
    }
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
    guard index < feedResults.count else { return }
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
    delegate?.removeThumbnails(for: indicesToCancel)
    for idx in indicesToCancel {
      let feed = feedResults[idx]
      HLSDownloadManager.shared.cancelDownload(for: feed.url)
      HLSDownloadManager.shared.clearMappingsAndCache(for: feed.url, assetTitle: feed.videoID)
      currentlyDownloadingIndexes.remove(idx)
    }
  }

  private func loadVideo(at index: Int) async throws -> AVPlayerItem? {
    guard index < feedResults.count else { return nil }
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

extension FeedsPlayer: HLSDownloadManagerProtocol {
  nonisolated func clearedCache(for assetTitle: String) {
    Task { @MainActor [weak self] in
      guard let self else { return }
      guard let index = feedResults.firstIndex(where: { $0.videoID == assetTitle }) else { return }
      self.playerItems.removeValue(forKey: index)
      self.delegate?.cacheCleared(atc: index)
    }
  }
}

protocol FeedsPlayerProtocol: AnyObject {
  func cacheCleared(atc index: Int)
  func removeThumbnails(for set: Set<Int>)
}

extension FeedsPlayer {
  enum Constants {
    static let radius = 5
  }
}
