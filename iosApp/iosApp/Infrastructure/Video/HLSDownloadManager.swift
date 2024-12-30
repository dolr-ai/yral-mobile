//
//  HLSDownloadManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import AVFoundation
import Network

@MainActor
final class HLSDownloadManager: NSObject {
  static let shared = HLSDownloadManager()
  weak var delegate: HLSDownloadManagerProtocol?

  private override init() {
    super.init()
    createDownloadSession()
    monitorNetwork()
  }

  private var downloadSession: AVAssetDownloadURLSession!
  private var activeDownloads: [URL: AVAssetDownloadTask] = [:]
  private var assetTitleForURL: [URL: String] = [:]
  private var localRemoteUrlMapping: [URL: URL] = [:]
  private var downloadContinuations: [URL: CheckedContinuation<URL, Error>] = [:]
  private var downloadedAssetsLRU: [String: Date] = [:]

  private let monitor = NWPathMonitor()
  private var isNetworkAvailable = true

  private func createDownloadSession() {
    let config = URLSessionConfiguration.background(withIdentifier: Constants.downloadIdentifier)
    config.httpMaximumConnectionsPerHost = Constants.maxConnectionsPerHost

    downloadSession = AVAssetDownloadURLSession(configuration: config,
                                                assetDownloadDelegate: self,
                                                delegateQueue: .main)
  }

  private func monitorNetwork() {
    monitor.pathUpdateHandler = { [weak self] path in
      Task { @MainActor in
        self?.isNetworkAvailable = (path.status == .satisfied)
      }
    }
    monitor.start(queue: DispatchQueue.global(qos: .background))
  }

  func startDownloadAsync(
    hlsURL: URL,
    assetTitle: String
  ) async throws -> URL {
    if activeDownloads[hlsURL] != nil {
      print("Download already in progress for: \(hlsURL.absoluteString)")
    }
    let asset = AVURLAsset(url: hlsURL)
    let downloadConfig = AVAssetDownloadConfiguration(asset: asset, title: assetTitle)
    let downloadTask = downloadSession.makeAssetDownloadTask(downloadConfiguration: downloadConfig)
    activeDownloads[hlsURL] = downloadTask
    assetTitleForURL[hlsURL] = assetTitle

    return try await withCheckedThrowingContinuation { continuation in
      downloadContinuations[hlsURL] = continuation
      downloadTask.resume()
      print("Started download for \(hlsURL.absoluteString), assetTitle=\(assetTitle)")
    }
  }

  func createLocalAssetIfAvailable(for hlsURL: URL) -> AVURLAsset? {
    guard let assetTitle = assetTitleForURL[hlsURL], let localURL = localRemoteUrlMapping[hlsURL] else {
      return nil
    }
    downloadedAssetsLRU[assetTitle] = Date()
    let localAsset = AVURLAsset(url: localURL)
    return localAsset
  }

  private func enforceCacheLimitIfNeeded() {
    while downloadedAssetsLRU.count >= Constants.maxOfflineAssets {
      let sorted = downloadedAssetsLRU.sorted { $0.value < $1.value }
      guard let oldest = sorted.first else { break }
      if let hlsURL = assetTitleForURL.first(where: { $0.value == oldest.key })?.key {
        cancelDownload(for: hlsURL)         // if still downloading
        clearMappingsAndCache(for: hlsURL, assetTitle: oldest.key)
      }
    }
  }

  func cancelDownload(for hlsURL: URL) {
    if let task = activeDownloads[hlsURL] {
      task.cancel()
      activeDownloads.removeValue(forKey: hlsURL)
      downloadContinuations[hlsURL]?.resume(throwing: CancellationError())
      downloadContinuations[hlsURL] = nil
      print("Canceled ongoing download for \(hlsURL.absoluteString)")
    }
  }

  func clearMappingsAndCache(for hlsURL: URL, assetTitle: String) {
    guard let localURL = localRemoteUrlMapping[hlsURL] else { return }
    removeAsset(localURL)
    localRemoteUrlMapping.removeValue(forKey: hlsURL)
    downloadedAssetsLRU.removeValue(forKey: assetTitle)
    self.delegate?.clearedCache(for: assetTitle)
  }

  private func removeAsset(_ url: URL) {
    do {
      if FileManager.default.fileExists(atPath: url.path) {
        try FileManager.default.removeItem(at: url)
        print("Removed asset from disk: \(url)")
      }
    } catch {
      print("Failed to remove asset: \(error)")
    }
  }
}

extension HLSDownloadManager: AVAssetDownloadDelegate {
  nonisolated func urlSession(_ session: URLSession,
                              assetDownloadTask: AVAssetDownloadTask,
                              didFinishDownloadingTo location: URL) {
    Task { @MainActor [weak self] in
      guard let self = self else { return }
      let matchingEntry = self.activeDownloads.first { $0.value == assetDownloadTask }
      guard let (feedURL, _) = matchingEntry else { return }
      defer {
        enforceCacheLimitIfNeeded()
        self.activeDownloads.removeValue(forKey: feedURL)
        self.downloadContinuations.removeValue(forKey: feedURL)
      }
      self.downloadContinuations[feedURL]?.resume(returning: location)
      print("Finished writing to location: \(location)")
    }
  }

  nonisolated func urlSession(
    _ session: URLSession,
    assetDownloadTask: AVAssetDownloadTask,
    willDownloadTo location: URL
  ) {
    Task { @MainActor [weak self] in
      guard let self = self else { return }
      let matchingEntry = self.activeDownloads.first { $0.value == assetDownloadTask }
      guard let (feedURL, _) = matchingEntry else { return }
      self.localRemoteUrlMapping[feedURL] = location
      print("Started writing to location: \(location)")
    }
  }
}

extension HLSDownloadManager {
  enum Constants {
    static let maxOfflineAssets = 10
    static let maxConnectionsPerHost = 5
    static let downloadIdentifier = "com.yral.HLSDownloadManager.async"
  }
}

protocol HLSDownloadManagerProtocol: AnyObject {
  func clearedCache(for assetTitle: String)
}
