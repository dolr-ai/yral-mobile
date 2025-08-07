//
//  YralPlayer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation

@MainActor
protocol YralPlayer {
  /// Checks/Sets player visibility
  var isPlayerVisible: Bool { get set }

  /// Gets the current index of video
  var currentIndex: Int { get }

  /// Gets the instance of player
  var player: YralQueuePlayer { get }

  /// Number of videos to preload
  var preloadRadius: Int { get }

  /// Plays the video
  func play()

  /// Pauses the video
  func pause()

  /// Loads initial videos to the player
  func loadInitialVideos(_ feeds: [FeedResult])

  /// Adds more videos to the player
  func addFeedResults(_ feeds: [FeedResult])

  /// Advances to any video on the basis of passed index
  func advanceToVideo(at index: Int)

  /// Removes videos from the player
  func removeFeeds(_ feeds: [FeedResult])

  /// Preload next few feeds
  func preloadFeeds(for indices: [Int]) async

  /// Cancels preload if scrolled too fast
  func cancelPreloadOutsideRange(for indices: [Int]) async
}
