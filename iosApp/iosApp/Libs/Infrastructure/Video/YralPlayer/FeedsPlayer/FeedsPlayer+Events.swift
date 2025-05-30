//
//  FeedsPlayer+Events.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation

extension FeedsPlayer {

  func stop(startupMonitor: PerformanceMonitor, isSuccess: Bool) {
    startupMonitor.setMetadata(
      key: Constants.performanceResultKey,
      value: isSuccess ? Constants.performanceSuccessKey : Constants.performanceErrorKey
    )
    startupMonitor.stop()
  }

  func stopPlaybackMonitor() {
    if let monitor = playbackMonitor {
      monitor.setMetadata(key: Constants.performanceResultKey, value: Constants.performanceSuccessKey)
      monitor.stop()
      playbackMonitor = nil
    }
  }

  func startTimeControlObservations(_ player: AVQueuePlayer) {
    timeControlObservation = player.observe(
      \AVQueuePlayer.timeControlStatus,
       options: [.initial, .new]
    ) { [weak self] _, change in
      guard let self else { return }
      let status = change.newValue ?? player.timeControlStatus
      Task { @MainActor [weak self] in
        self?.startTimerMonitoring(status)
      }
    }
  }

  func startTimerMonitoring(_ newStatus: AVPlayer.TimeControlStatus) {
    switch newStatus {
    case .playing:
      finishFirstFrameTrace(success: true)
      if let videoID = self.feedResults[safe: self.currentIndex]?.videoID {
        self.playbackMonitor = FirebasePerformanceMonitor(traceName: Constants.videoPlaybackTrace)
        self.playbackMonitor?.setMetadata(key: Constants.videoIDKey, value: videoID)
        self.playbackMonitor?.start()
        self.playbackMonitor?.incrementMetric(Constants.rebufferTimeMetric, by: .zero)
        self.playbackMonitor?.incrementMetric(Constants.rebufferCountMetric, by: .zero)
      }
      if let stallStart = self.stallStart {
        let timeMs = Int64(Date().timeIntervalSince(stallStart) * CGFloat.thousand)
        self.playbackMonitor?.incrementMetric(Constants.rebufferTimeMetric, by: timeMs)
        self.stallStart = nil
      }
    case .waitingToPlayAtSpecifiedRate:
      break
    default: break
    }
  }

  @objc func handlePlaybackStalled(_ note: Notification) {
    playbackMonitor?.incrementMetric(Constants.rebufferCountMetric, by: Int64(.one))
    stallStart = Date()
  }

  func attachTimeObserver() {
    if let token = timeObserver {
      (player as? AVQueuePlayer)?.removeTimeObserver(token)
      timeObserver = nil
    }
    guard let queue = player as? AVQueuePlayer else { return }
    let interval = CMTime(
      seconds: Constants.videoDurationEventMinThreshold,
      preferredTimescale: CMTimeScale(Constants.timescaleEventSampling)
    )
    timeObserver = queue.addPeriodicTimeObserver(
      forInterval: interval, queue: .main
    ) { [weak self] currentTime in
      self?.evaluateProgress(currentTime)
    }
  }

  func evaluateProgress(_ time: CMTime) {
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

  func finishFirstFrameTrace(success: Bool) {
    if let ffm = firstFrameMonitor {
      ffm.setMetadata(
        key: Constants.performanceResultKey,
        value: success ? Constants.performanceSuccessKey
        : Constants.performanceErrorKey
      )
      ffm.stop()
      firstFrameMonitor = nil
    }
  }
}

enum PlaybackMilestone {
  case started
  case almostFinished
}
