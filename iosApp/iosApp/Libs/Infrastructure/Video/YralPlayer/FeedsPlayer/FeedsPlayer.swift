//  FeedsPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.

import UIKit
import AVFoundation

@MainActor
final class FeedsPlayer: YralPlayer {
  var feedResults: [FeedResult] = []
  var currentIndex: Int = .zero
  var player: YralQueuePlayer
  var hlsDownloadManager: HLSDownloadManaging

  var playerItems: [String: AVPlayerItem] = [:] {
    didSet {
      onPlayerItemsChanged?(playerItems.keys.filter { oldValue[$0] == nil }.first, playerItems.count)
    }
  }
  private var lastPlayedTimes: [String: CMTime] = [:]
  private var currentlyDownloadingIDs: Set<String> = []

  private var playerLooper: AVPlayerLooper?

  var isPlayerVisible: Bool = true
  var didEmptyFeeds: (() -> Void)?
  var onPlayerItemsChanged: ((String?, Int) -> Void)?
  weak var delegate: FeedsPlayerProtocol?

  private var timeObserver: Any?
  private var startLogged = Set<Int>()
  private var finishLogged = Set<Int>()

  init(player: YralQueuePlayer = AVQueuePlayer(), hlsDownloadManager: HLSDownloadManaging) {
    self.player = player
    self.hlsDownloadManager = hlsDownloadManager
  }

  func loadInitialVideos(_ feeds: [FeedResult]) {
    self.feedResults = feeds
    configureAudioSession()
    currentIndex = .zero
    Task {
      await hlsDownloadManager.setDelegate(self)
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

  private func attachTimeObserver() {
    if let token = timeObserver {
      (player as? AVQueuePlayer)?.removeTimeObserver(token)
      timeObserver = nil
    }
    guard let queue = player as? AVQueuePlayer else { return }
    let interval = CMTime(
      seconds: Constants.videoDurationEventMinThreshold,
      preferredTimescale: CMTimeScale(Constants.timescaleEventSamping)
    )
    timeObserver = queue.addPeriodicTimeObserver(
      forInterval: interval, queue: .main
    ) { [weak self] currentTime in
      self?.evaluateProgress(currentTime)
    }
  }

  private func evaluateProgress(_ time: CMTime) {
    guard currentIndex < feedResults.count,
          let item = player.currentItem,
          item.status == .readyToPlay else { return }

    let seconds  = time.seconds
    let duration = item.duration.seconds
    if seconds >= CGFloat.pointOne, startLogged.insert(currentIndex).inserted {
      delegate?.reachedPlaybackMilestone(.started, for: currentIndex)
    }

    if duration.isFinite,
       seconds / duration >= Constants.videoDurationEventMaxThreshold,
       finishLogged.insert(currentIndex).inserted {
      delegate?.reachedPlaybackMilestone(.almostFinished, for: currentIndex)
    }
  }

  func advanceToVideo(at index: Int) {
    guard index >= 0 && index < feedResults.count && currentIndex < feedResults.count else { return }
    if let currentTime = player.currentItem?.currentTime() {
      let currentVideoID = feedResults[currentIndex].videoID
      lastPlayedTimes[currentVideoID] = currentTime
    }
    if abs(index - currentIndex) > Constants.radius {
      Task {
        await cancelPreloadOutsideRange(center: index, radius: Constants.radius)
      }
    }

    startLogged.remove(index)
    finishLogged.remove(index)

    currentIndex = index
    Task {
      let currentVideoID = feedResults[index].videoID
      if let preloadedItem = playerItems[currentVideoID] {
        startLooping(with: preloadedItem)
      } else {
        await prepareCurrentVideo()
      }
    }
  }

  func removeFeeds(_ feeds: [FeedResult]) {
    guard !feeds.isEmpty else { return }
    let removedVideoIDs = Set(feeds.map { $0.videoID })
    let currentFeedID = feedResults.indices.contains(currentIndex)
    ? feedResults[currentIndex].videoID
    : nil // CHANGED

    feedResults.removeAll(where: { removedVideoIDs.contains($0.videoID) })
    removedVideoIDs.forEach {
      playerItems.removeValue(forKey: $0)
      lastPlayedTimes.removeValue(forKey: $0)
    }

    if let currentFeedID = currentFeedID,
       !feedResults.contains(where: { $0.videoID == currentFeedID }) {
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

    guard let player = player as? AVQueuePlayer else {
      player.play()
      Task {
        await preloadFeeds()
      }
      return
    }

    playerLooper = AVPlayerLooper(player: player, templateItem: item)
    attachTimeObserver()

    let currentVideoID = feedResults[currentIndex].videoID
    if let lastTime = lastPlayedTimes[currentVideoID] {
      player.seek(to: lastTime, toleranceBefore: .zero, toleranceAfter: .zero) { [weak self] _ in
        Task { @MainActor [weak self] in
          self?.play()
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
    let endIndex = min(feedResults.count, currentIndex + Constants.radius)

    for index in startIndex..<endIndex {
      let feed = feedResults[index]
      let videoID = feed.videoID
      guard playerItems[videoID] == nil else { continue }
      guard !currentlyDownloadingIDs.contains(videoID) else { continue }
      await downloadVideo(at: index)
    }
  }

  private func downloadVideo(at index: Int) async {
    guard index < feedResults.count else { return }
    let feed = feedResults[index]
    let videoID = feed.videoID
    let assetTitle = videoID

    currentlyDownloadingIDs.insert(videoID)

    do {
      _ = try await hlsDownloadManager.startDownloadAsync(
        hlsURL: feed.url,
        assetTitle: assetTitle
      )
      if let item = try await loadVideo(at: index) {
        if !playerItems.keys.contains(videoID) {
          playerItems[videoID] = item
        }
      }
    } catch is CancellationError {
      print("Preload canceled for index \(index).")
    } catch {
      print("Preload failed for index \(index): \(error)")
    }

    currentlyDownloadingIDs.remove(videoID)
  }

  private func cancelPreloadOutsideRange(center: Int, radius: Int) async {
    let validIDs = Set((max(center - radius, 0)...min(center + radius, feedResults.count - 1))
      .map { feedResults[$0].videoID })

    let idsToCancel = currentlyDownloadingIDs.subtracting(validIDs)
    let indicesToCancel = Set(idsToCancel.compactMap { id in
      feedResults.firstIndex { $0.videoID == id }
    })
    delegate?.removeThumbnails(for: indicesToCancel)

    for id in idsToCancel {
      guard let feed = feedResults.first(where: { $0.videoID == id }) else { continue }
      await hlsDownloadManager.cancelDownload(for: feed.url)
      await hlsDownloadManager.clearMappingsAndCache(for: feed.url, assetTitle: id)
      currentlyDownloadingIDs.remove(id)
    }
  }

  private func loadVideo(at index: Int) async throws -> AVPlayerItem? {
    guard index < feedResults.count else { return nil }
    let feed = feedResults[index]
    let videoID = feed.videoID
    if let localAsset = await hlsDownloadManager.createLocalAssetIfAvailable(for: feed.url) {
      do {
        try await localAsset.loadPlayableAsync()
        let item = AVPlayerItem(asset: localAsset)
        playerItems[videoID] = item
        return item
      } catch {
        print("Local asset not playable (fallback to remote). Error: \(error)")
      }
    }
    let remoteAsset = AVURLAsset(url: feed.url)
    try await remoteAsset.loadPlayableAsync()
    let item = AVPlayerItem(asset: remoteAsset)
    playerItems[videoID] = item
    return item
  }

  deinit {
    if let token = timeObserver {
      (player as? AVQueuePlayer)?.removeTimeObserver(token)
    }
  }
}

extension FeedsPlayer: HLSDownloadManagerProtocol {
  nonisolated func clearedCache(for assetTitle: String) {
    Task { @MainActor [weak self] in
      guard let self else { return }
      self.playerItems.removeValue(forKey: assetTitle)
      self.lastPlayedTimes.removeValue(forKey: assetTitle)
      guard let index = feedResults.firstIndex(where: { $0.videoID == assetTitle }) else { return }
      self.delegate?.cacheCleared(atc: index)
    }
  }
}

protocol FeedsPlayerProtocol: AnyObject {
  func cacheCleared(atc index: Int)
  func removeThumbnails(for set: Set<Int>)
  func reachedPlaybackMilestone(
    _ milestone: PlaybackMilestone,
    for index: Int
  )
}

extension FeedsPlayer {
  enum Constants {
    static let radius = 5
    static let videoDurationEventMinThreshold = 0.05
    static let videoDurationEventMaxThreshold = 0.95
    static let timescaleEventSamping = 600.0
  }
}

enum PlaybackMilestone {
  case started
  case almostFinished
}
