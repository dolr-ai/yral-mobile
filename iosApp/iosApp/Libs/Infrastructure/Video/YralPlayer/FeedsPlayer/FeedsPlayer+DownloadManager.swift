//
//  FeedsPlayer+DownloadManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import AVFoundation

extension FeedsPlayer {
  nonisolated func downloadManager(
    _ manager: any HLSDownloadManaging,
    didBeginAssetFor remoteURL: URL,
    tempDirURL: URL,
    assetTitle: String
  ) {
    Task { @MainActor [weak self] in
      guard let self else { return }

      let tempAsset = AVURLAsset(url: tempDirURL)

      do {
        try await tempAsset.loadPlayableAsync()
      } catch {
        crashReporter.recordException(error)
        return
      }

      let item = AVPlayerItem(asset: tempAsset)
      item.preferredForwardBufferDuration = CGFloat.half
      item.canUseNetworkResourcesForLiveStreamingWhilePaused = true

      playerItems[assetTitle] = item

      if let idx = feedResults.firstIndex(where: { $0.videoID == assetTitle }),
         idx == currentIndex {
        try? await startLooping(with: item)
      }
    }
  }

  nonisolated func downloadManager(
    _ manager: any HLSDownloadManaging,
    didFinishAssetFor remoteURL: URL,
    localFileURL: URL,
    assetTitle: String
  ) {
    Task { @MainActor [weak self] in
      guard let self else { return }

      let localAsset = AVURLAsset(url: localFileURL)
      do { try await localAsset.loadPlayableAsync() } catch {
        crashReporter.recordException(error)
        return
      }

      let newItem = AVPlayerItem(asset: localAsset)
      newItem.preferredForwardBufferDuration = CGFloat.half
      newItem.canUseNetworkResourcesForLiveStreamingWhilePaused = true
      playerItems[assetTitle] = newItem
    }
  }
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
