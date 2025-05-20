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

actor HLSDownloadManager: NSObject, HLSDownloadManaging {

  private weak var _delegate: HLSDownloadManagerProtocol?
  var delegate: HLSDownloadManagerProtocol? {
    get { _delegate }
    set { _delegate = newValue }
  }

  private let networkMonitor: NetworkMonitorProtocol
  private let fileManager: FileManager

  private var _downloadSession: AVAssetDownloadURLSessionProtocol!
  private var downloadSession: AVAssetDownloadURLSessionProtocol {
    _downloadSession
  }

  var activeDownloads: [URL: AVAssetDownloadTaskProtocol] = [:]
  var assetTitleForURL: [URL: String] = [:]
  var downloadContinuations: [URL: CheckedContinuation<URL, Error>] = [:]
  var downloadedAssetsLRU: [String: Date] = [:]
  var inflightAssetForURL: [URL: AVURLAsset] = [:]
  private var downloadMonitors: [URL: PerformanceMonitor] = [:]

  private let userDefaults = UserDefaults.standard
  private static let bookmarksKey = "HLSBookmarks"
  private static let traceKey = "HLSDownload"

  private lazy var storedBookmarks: [String: Data] = {
    return (userDefaults.dictionary(forKey: Self.bookmarksKey) as? [String: Data]) ?? [:]
  }()

  init(networkMonitor: NetworkMonitorProtocol,
       fileManager: FileManager) {
    self.networkMonitor = networkMonitor
    self.fileManager = fileManager
    super.init()

    let config = URLSessionConfiguration.background(withIdentifier: UUID().uuidString)
    config.httpMaximumConnectionsPerHost = Constants.maxConnectionsPerHost
    let session = AVAssetDownloadURLSession(
      configuration: config,
      assetDownloadDelegate: self,
      delegateQueue: .main
    )
    self._downloadSession = DefaultAssetDownloadURLSession(session: session)
  }

  override init() {
    self.init(networkMonitor: DefaultNetworkMonitor(), fileManager: .default)
  }

  init(downloadSession: AVAssetDownloadURLSessionProtocol,
       networkMonitor: NetworkMonitorProtocol,
       fileManager: FileManager) {
    self._downloadSession = downloadSession
    self.networkMonitor = networkMonitor
    self.fileManager = fileManager
    super.init()
    self.networkMonitor.startMonitoring()
  }

  func setDelegate(_ delegate: HLSDownloadManagerProtocol?) {
    self._delegate = delegate
  }

  func startDownloadAsync(hlsURL: URL, assetTitle: String) async throws -> URL {
    removeBookmark(for: assetTitle)
    if activeDownloads[hlsURL] != nil {
      print("Download already in progress for: \(hlsURL.absoluteString)")
    }
    let asset = AVURLAsset(url: hlsURL)
    inflightAssetForURL[hlsURL] = asset
    let downloadConfig = AVAssetDownloadConfiguration(asset: asset, title: assetTitle)
    guard let downloadTask = downloadSession.makeAssetDownloadTask(downloadConfiguration: downloadConfig) else {
      throw URLError(.badURL)
    }
    activeDownloads[hlsURL] = downloadTask
    assetTitleForURL[hlsURL] = assetTitle

    let monitor: PerformanceMonitor = FirebasePerformanceMonitor(traceName: HLSDownloadManager.traceKey)
    monitor.setMetadata(key: Constants.assetTitleKey, value: assetTitle)
    monitor.start()
    downloadMonitors[hlsURL] = monitor

    return try await withCheckedThrowingContinuation { continuation in
      downloadContinuations[hlsURL] = continuation
      downloadTask.resume()
      print("Started download for \(hlsURL.absoluteString), assetTitle=\(assetTitle)")
    }
  }

  func createLocalAssetIfAvailable(for hlsURL: URL) -> AVURLAsset? {
    guard
      let assetTitle = assetTitleForURL[hlsURL]
    else {
      return nil
    }

    if let localAssetURL = resolveBookmarkIfPresent(for: assetTitle) {
      downloadedAssetsLRU[assetTitle] = Date()
      return AVURLAsset(url: localAssetURL)
    }

    return nil
  }

  func localOrInflightAsset(for hlsURL: URL) -> AVURLAsset? {
    if let asset = createLocalAssetIfAvailable(for: hlsURL) { return asset }
    return inflightAssetForURL[hlsURL]
  }

  func cancelDownload(for hlsURL: URL) {
    if let task = activeDownloads[hlsURL] {
      task.cancel()
      activeDownloads.removeValue(forKey: hlsURL)
      inflightAssetForURL.removeValue(forKey: hlsURL)
      if let continuation = downloadContinuations.removeValue(forKey: hlsURL) {
        continuation.resume(throwing: CancellationError())
      }
      if let monitor = downloadMonitors.removeValue(forKey: hlsURL) {
        monitor.setMetadata(key: Constants.performanceResultKey, value: Constants.performanceCancelKey)
        monitor.stop()
      }
      print("Canceled ongoing download for \(hlsURL.absoluteString)")
    }
  }

  func elevatePriority(for url: URL) {
    if let task = activeDownloads[url] {
      task.underlyingTask?.priority = URLSessionTask.defaultPriority
    }
  }

  func clearMappingsAndCache(for hlsURL: URL, assetTitle: String) {
    removeBookmark(for: assetTitle)
    downloadedAssetsLRU.removeValue(forKey: assetTitle)
    delegate?.clearedCache(for: assetTitle)
  }

  func removeAsset(_ url: URL) {
    Task.detached(priority: .background) {
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

  private func enforceCacheLimitIfNeeded() {
    while downloadedAssetsLRU.count >= Constants.maxOfflineAssets {
      let sorted = downloadedAssetsLRU.sorted { $0.value < $1.value }
      guard let oldest = sorted.first else { break }
      if let hlsURL = assetTitleForURL.first(where: { $0.value == oldest.key })?.key {
        cancelDownload(for: hlsURL)
        clearMappingsAndCache(for: hlsURL, assetTitle: oldest.key)
      }
    }
  }

  private func onStartDownload(
    _ session: URLSession,
    assetDownloadTask: AVAssetDownloadTask,
    didFinishDownloadingTo location: URL
  ) {
    let matchingEntry = self.activeDownloads.first {
      $0.value.underlyingTask === assetDownloadTask
    }
    guard let (feedURL, _) = matchingEntry else { return }

    print("Started writing to location: \(location)")
  }

  private func onEndDownload(_ session: URLSession,
                             assetDownloadTask: AVAssetDownloadTask,
                             didFinishDownloadingTo location: URL) {
    let matchingEntry = self.activeDownloads.first {
      $0.value.underlyingTask === assetDownloadTask
    }
    guard let (feedURL, _) = matchingEntry else { return }
    defer {
      self.enforceCacheLimitIfNeeded()
      self.activeDownloads.removeValue(forKey: feedURL)
      inflightAssetForURL.removeValue(forKey: feedURL)
    }

    if let assetTitle = assetTitleForURL[feedURL] {
      storeBookmark(for: assetTitle, localFileURL: location)
    }

    if let continuation = self.downloadContinuations.removeValue(forKey: feedURL) {
      continuation.resume(returning: location)
    }
    if let monitor = downloadMonitors.removeValue(forKey: feedURL) {
      monitor.setMetadata(key: Constants.performanceResultKey, value: Constants.performanceSuccessKey)
      monitor.stop()
    }
    print("Finished writing to location: \(location)")
  }

  fileprivate func handleDownloadError(_ url: URL, error: Error?) {
    if let monitor = self.downloadMonitors.removeValue(forKey: url) {
      monitor.setMetadata(key: Constants.performanceResultKey, value: Constants.performanceErrorKey)
      monitor.stop()
    }

    if let continuation = downloadContinuations.removeValue(forKey: url) {
      continuation.resume(throwing: error ?? URLError(.unknown))
    }

    activeDownloads.removeValue(forKey: url)
    inflightAssetForURL.removeValue(forKey: url)
    enforceCacheLimitIfNeeded()
  }
}

extension HLSDownloadManager: AVAssetDownloadDelegate {

  nonisolated func urlSession(_ session: URLSession,
                              assetDownloadTask: AVAssetDownloadTask,
                              willDownloadTo location: URL) {
    Task { [weak self] in
      guard let self = self else { return }
      await self.onStartDownload(
        session,
        assetDownloadTask: assetDownloadTask,
        didFinishDownloadingTo: location
      )
    }
  }

  nonisolated func urlSession(
    _ session: URLSession,
    assetDownloadTask: AVAssetDownloadTask,
    didFinishDownloadingTo location: URL
  ) {
    Task { [weak self] in
      guard let self = self else { return }
      await self.onEndDownload(
        session,
        assetDownloadTask: assetDownloadTask,
        didFinishDownloadingTo: location
      )
    }
  }

  nonisolated func urlSession(
    _ session: URLSession,
    task: URLSessionTask,
    didCompleteWithError error: (any Error)?
  ) {
    guard let assetTask = task as? AVAssetDownloadTask else { return }
    Task { [weak self] in
      guard let self,
            let url = await self.activeDownloads.first(where: { $0.value.underlyingTask === assetTask })?.key
      else { return }
      await handleDownloadError(url, error: error)
    }
  }
}

extension HLSDownloadManager {
  private func storeBookmark(for assetTitle: String, localFileURL: URL) {
    do {
      let bookmarkData = try localFileURL.bookmarkData()
      storedBookmarks[assetTitle] = bookmarkData
      userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
      userDefaults.synchronize()
    } catch {
      print("Failed to create bookmark for \(assetTitle): \(error)")
    }
  }

  private func removeBookmark(for assetTitle: String) {
    storedBookmarks.removeValue(forKey: assetTitle)
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }

  private func resolveBookmarkIfPresent(for assetTitle: String) -> URL? {
    guard let bookmarkData = storedBookmarks[assetTitle] else { return nil }
    var isStale = false
    do {
      let resolvedURL = try URL(resolvingBookmarkData: bookmarkData,
                                bookmarkDataIsStale: &isStale)
      if isStale {
        print("Bookmark data was stale for \(assetTitle)")
      }
      return resolvedURL
    } catch {
      print("Failed to resolve bookmark for \(assetTitle): \(error)")
      return nil
    }
  }

  func removeAllBookmarkedAssetsOnLaunch() {
    for (assetTitle, bookmarkData) in storedBookmarks {
      do {
        var stale = false
        let url = try URL(resolvingBookmarkData: bookmarkData, bookmarkDataIsStale: &stale)
        if fileManager.fileExists(atPath: url.path) {
          try fileManager.removeItem(at: url)
          print("Removed leftover HLS file: \(url.lastPathComponent)")
        }
      } catch {
        print("Error removing leftover asset for \(assetTitle): \(error)")
      }
      storedBookmarks.removeValue(forKey: assetTitle)
    }
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }
}

extension HLSDownloadManager {
  enum Constants {
    static let maxOfflineAssets = 10
    static let maxConnectionsPerHost = 3
    static let downloadIdentifier = "com.yral.HLSDownloadManager.async"
    static let videoKey = "userVideos"
    static let assetTitleKey = "asset_title"
    static let performanceResultKey = "result"
    static let performanceErrorKey = "error"
    static let performanceSuccessKey = "success"
    static let performanceCancelKey = "cancelled"
  }
}

protocol HLSDownloadManagerProtocol: AnyObject {
  func clearedCache(for assetTitle: String)
}

protocol AVAssetDownloadTaskProtocol: AnyObject {
  func resume()
  func cancel()
  var underlyingTask: AVAssetDownloadTask? { get }
}

protocol AVAssetDownloadURLSessionProtocol: AnyObject {
  func makeAssetDownloadTask(downloadConfiguration: AVAssetDownloadConfiguration) -> AVAssetDownloadTaskProtocol?
}

final class DefaultAssetDownloadTask: AVAssetDownloadTaskProtocol {
  private let realTask: AVAssetDownloadTask

  init(realTask: AVAssetDownloadTask) {
    self.realTask = realTask
  }

  func resume() {
    realTask.resume()
  }

  func cancel() {
    realTask.cancel()
  }

  var underlyingTask: AVAssetDownloadTask? {
    realTask
  }
}

final class DefaultAssetDownloadURLSession: AVAssetDownloadURLSessionProtocol {
  private let session: AVAssetDownloadURLSession

  init(session: AVAssetDownloadURLSession) {
    self.session = session
  }

  func makeAssetDownloadTask(downloadConfiguration: AVAssetDownloadConfiguration) -> AVAssetDownloadTaskProtocol? {
    let avTask = session.makeAssetDownloadTask(downloadConfiguration: downloadConfiguration)
    avTask.priority = URLSessionTask.lowPriority
    return DefaultAssetDownloadTask(realTask: avTask)
  }
}
