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

  func startDownloadAsync(hlsURL: URL, assetTitle: String) async throws -> URL
  func cancelDownload(for hls: URL)
  func clearMappingsAndCache(for hls: URL, assetTitle: String)
  func createLocalAssetIfAvailable(for hlsURL: URL) -> AVURLAsset?
  func setDelegate(_ delegate: HLSDownloadManagerProtocol?)
}
