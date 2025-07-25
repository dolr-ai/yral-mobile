//
//  HLSDownloadManaging.swift
//  iosApp
//
//  Created by Samarth Paboowal on 14/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation

protocol HLSDownloadManaging: Actor {
  var delegate: HLSDownloadManagerProtocol? { get set }
  var activeDownloads: [URL: AVAssetDownloadTaskProtocol] { get }

  func startDownloadAsync(hlsURL: URL, assetTitle: String) async throws -> URL
  func cancelDownload(for hls: URL)
  func clearMappingsAndCache(for hls: URL, assetTitle: String)
  func prefetch(url: URL, assetTitle: String) async
  func createLocalAssetIfAvailable(for hlsURL: URL) -> AVURLAsset?
  func localOrInflightAsset(for hlsURL: URL) -> AVURLAsset?
  func elevatePriority(for url: URL)
  func setDelegate(_ delegate: HLSDownloadManagerProtocol?)
}
