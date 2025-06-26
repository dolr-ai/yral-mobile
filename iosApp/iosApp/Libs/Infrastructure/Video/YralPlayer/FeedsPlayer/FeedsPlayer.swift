//  FeedsPlayer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.

import UIKit
import AVFoundation

// swiftlint: disable type_body_length
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
  private let networkMonitor: NetworkMonitorProtocol
  private let crashReporter: CrashReporter

  private var preloadRadius: Int {
    networkMonitor.isGoodForPrefetch ? Constants.radiusGoodNetwork : Constants.radiusbadNetwork
  }

  // MARK: Video duration event loggers
  var timeObserver: Any?
  var startLogged = Set<Int>()
  var finishLogged = Set<Int>()

  // MARK: Performance monitor
  var firstFrameMonitor: PerformanceMonitor?
  var playbackMonitor: PerformanceMonitor?
  var timeControlObservation: NSKeyValueObservation?
  var stallStart: Date?

  // MARK: Method implementations
  init(
    player: YralQueuePlayer = AVQueuePlayer(),
    hlsDownloadManager: HLSDownloadManaging,
    networkMonitor: NetworkMonitorProtocol,
    crashReporter: CrashReporter
  ) {
    self.player = player
    self.hlsDownloadManager = hlsDownloadManager
    self.networkMonitor = networkMonitor
    self.crashReporter = crashReporter
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleEULAAccepted(_:)),
      name: .eulaAcceptedChanged,
      object: nil
    )
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handlePlaybackStalled(_:)),
      name: .AVPlayerItemPlaybackStalled,
      object: nil
    )
    networkMonitor.startMonitoring()
//    (player as? AVQueuePlayer)?.automaticallyWaitsToMinimizeStalling = false
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
      updateMuteState()
      try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
      try AVAudioSession.sharedInstance().setActive(true)
    } catch {
      crashReporter.recordException(error)
      print("Failed to configure AVAudioSession: \(error)")
    }
  }

  func addFeedResults(_ feeds: [FeedResult]) {
    self.feedResults += feeds
    if self.feedResults.count <= preloadRadius {
      Task {
        await preloadFeeds()
      }
    }
  }

  @objc private func handleEULAAccepted(_ note: Notification) {
    updateMuteState()
  }

  private func updateMuteState() {
    let accepted: Bool? = UserDefaultsManager.shared.get(for: .eulaAccepted)
    player.isMuted = !(accepted ?? false)
  }

  func advanceToVideo(at index: Int) {
    guard index >= 0 && index < feedResults.count && currentIndex < feedResults.count else { return }
    if let currentTime = player.currentItem?.currentTime() {
      let currentVideoID = feedResults[currentIndex].videoID
      lastPlayedTimes[currentVideoID] = currentTime
    }
    Task {
      await cancelPreloadOutsideRange(center: index, radius: preloadRadius)
    }

    startLogged.remove(index)
    finishLogged.remove(index)

    currentIndex = index
    Task {
      let feed = feedResults[index]
      await self.hlsDownloadManager.elevatePriority(for: feed.url)
      let currentVideoID = feedResults[index].videoID
      if let preloadedItem = playerItems[currentVideoID] {
        do {
          try await startLooping(with: preloadedItem)
        } catch {
          crashReporter.recordException(error)
        }
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
    : nil

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
        try await startLooping(with: item)
      }
    } catch {
      crashReporter.recordException(error)
      print("Error loading current video at index \(currentIndex): \(error)")
    }
  }

  private func startLooping(with item: AVPlayerItem) async throws {
    finishFirstFrameTrace(success: false)
    if let videoID = feedResults[safe: currentIndex]?.videoID {
      firstFrameMonitor = FirebasePerformanceMonitor(traceName: Constants.firstFrameTrace)
      firstFrameMonitor?.setMetadata(key: Constants.videoIDKey, value: videoID)
      firstFrameMonitor?.start()
    }
    stopPlaybackMonitor()
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
    startTimeControlObservations(player)

    Task { @MainActor in
      let queueItem = await player.waitForFirstItem()
      do {
        try await queueItem.waitUntilReady()
//        try await player.prerollVideo(atRate: 0)
        NotificationCenter.default.post(
          name: .feedItemReady,
          object: self,
          userInfo: ["index": currentIndex]
        )
      } catch {
        crashReporter.recordException(error)
        print("Item failed to become ready: \(error)")
      }
      guard currentIndex < feedResults.count else { return }
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
    if (player as? AVQueuePlayer)?.timeControlStatus != .playing {
      finishFirstFrameTrace(success: false)
    }
  }

  private func preloadFeeds() async {
    guard !feedResults.isEmpty, currentIndex + .one < feedResults.count else { return }
    let endIndex = min(feedResults.count, currentIndex + preloadRadius)
    for index in currentIndex + .one..<endIndex {
      guard feedResults.indices.contains(index) else { continue }
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
    defer {
      currentlyDownloadingIDs.remove(videoID)
    }

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
      crashReporter.recordException(error)
      print("Preload failed for index \(index): \(error)")
    }
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

    let startupMonitor: PerformanceMonitor = FirebasePerformanceMonitor(traceName: Constants.videoStartTrace)
    startupMonitor.setMetadata(key: Constants.videoIDKey, value: videoID)
    startupMonitor.start()

    if let asset = await hlsDownloadManager.localOrInflightAsset(for: feed.url) {
      do {
        try await asset.loadPlayableAsync()
        let item = AVPlayerItem(asset: asset)
        item.preferredForwardBufferDuration = CGFloat.half
        item.canUseNetworkResourcesForLiveStreamingWhilePaused = true
        playerItems[videoID] = item
        stop(startupMonitor: startupMonitor, isSuccess: true)
        return item
      } catch {
        stop(startupMonitor: startupMonitor, isSuccess: false)
        crashReporter.recordException(error)
        print("Local asset not playable (fallback to remote). Error: \(error)")
        throw error
      }
    }

    let remoteAsset = AVURLAsset(url: feed.url)
    do {
      try await remoteAsset.loadPlayableAsync()
      let item = AVPlayerItem(asset: remoteAsset)
      item.preferredForwardBufferDuration = CGFloat.half
      item.canUseNetworkResourcesForLiveStreamingWhilePaused = true
      playerItems[videoID] = item
      stop(startupMonitor: startupMonitor, isSuccess: true)
      return item
    } catch {
      stop(startupMonitor: startupMonitor, isSuccess: false)
      crashReporter.recordException(error)
      throw error
    }
  }

  deinit {
    playbackMonitor?.stop()
    if let token = timeObserver {
      (player as? AVQueuePlayer)?.removeTimeObserver(token)
    }
    timeControlObservation?.invalidate()
    stallStart = nil
    NotificationCenter.default.removeObserver(self)
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
    static let videoDurationEventMinThreshold = 0.05
    static let videoDurationEventMaxThreshold = 0.95
    static let timescaleEventSampling = 600.0
    static let radiusGoodNetwork = 5
    static let radiusbadNetwork = 3
    static let videoStartTrace = "VideoStartup"
    static let firstFrameTrace = "FirstFrame"
    static let videoPlaybackTrace = "VideoPlayback"
    static let videoIDKey = "video_id"
    static let performanceResultKey = "result"
    static let performanceErrorKey = "error"
    static let performanceSuccessKey = "success"
    static let rebufferTimeMetric = "rebuffer_time_ms"
    static let rebufferCountMetric = "rebuffer_count"
  }
}
// swiftlint: enable type_body_length
