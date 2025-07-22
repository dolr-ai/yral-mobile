//
//  MP4DownloadManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation
import Network

actor MP4DownloadManager: NSObject, MP4DownloadManaging {

  private weak var _delegate: MP4DownloadManagerProtocol?
  var delegate: MP4DownloadManagerProtocol? {
    get { _delegate }
    set { _delegate = newValue }
  }

  private let networkMonitor: NetworkMonitorProtocol
  private let fileManager: FileManager
  private let crashReporter: CrashReporter

  private var _downloadSession: URLSessionDownloadURLSessionProtocol!
  private var downloadSession: URLSessionDownloadURLSessionProtocol { _downloadSession }

  var activeDownloads: [URL: URLSessionDownloadTaskProtocol] = [:]
  var assetTitleForURL: [URL: String] = [:]
  var downloadContinuations: [URL: CheckedContinuation<URL, Error>] = [:]
  var downloadedAssetsLRU: [String: Date] = [:]
  var inflightAssetForURL: [URL: AVURLAsset] = [:]
  private var downloadMonitors: [URL: PerformanceMonitor] = [:]

  private let userDefaults = UserDefaults.standard
  private static let bookmarksKey = "MP4Bookmarks"
  private static let traceKey = "MP4Download"

  private lazy var storedBookmarks: [String: Data] = {
    (userDefaults.dictionary(forKey: Self.bookmarksKey) as? [String: Data]) ?? [:]
  }()

  init(
    networkMonitor: NetworkMonitorProtocol,
    fileManager: FileManager,
    crashReporter: CrashReporter
  ) {
    self.networkMonitor = networkMonitor
    self.fileManager = fileManager
    self.crashReporter = crashReporter
    super.init()

    let cfg = URLSessionConfiguration.background(withIdentifier: UUID().uuidString)
    cfg.httpMaximumConnectionsPerHost = Constants.maxConnectionsPerHost
    cfg.timeoutIntervalForRequest = Constants.requestTimeout
    cfg.timeoutIntervalForResource = Constants.resourceTimeout

    let session = URLSession(configuration: cfg, delegate: self, delegateQueue: .main)
    self._downloadSession = DefaultURLSessionDownloadURLSession(session: session)
  }

  override init() {
    self.networkMonitor = DefaultNetworkMonitor()
    self.fileManager = .default
    self.crashReporter = FirebaseCrashlyticsReporter()
    super.init()

    let cfg = URLSessionConfiguration.background(withIdentifier: UUID().uuidString)
    cfg.httpMaximumConnectionsPerHost = Constants.maxConnectionsPerHost

    let session = URLSession(configuration: cfg, delegate: self, delegateQueue: .main)
    self._downloadSession = DefaultURLSessionDownloadURLSession(session: session)
  }

  init(
    downloadSession: URLSessionDownloadURLSessionProtocol,
    networkMonitor: NetworkMonitorProtocol,
    fileManager: FileManager,
    crashReporter: CrashReporter
  ) {
    self._downloadSession = downloadSession
    self.networkMonitor = networkMonitor
    self.fileManager = fileManager
    self.crashReporter = crashReporter
    super.init()
  }

  func setDelegate(_ delegate: MP4DownloadManagerProtocol?) {
    self._delegate = delegate
  }

  func startDownloadAsync(mp4URL: URL, assetTitle: String) async throws -> URL {
    removeBookmark(for: assetTitle)

    if activeDownloads[mp4URL] != nil {
      print("Download already in progress for: \(mp4URL)")
      return try await withCheckedThrowingContinuation { continuation in
        downloadContinuations[mp4URL] = continuation
      }
    }

    inflightAssetForURL[mp4URL] = AVURLAsset(url: mp4URL)
    let monitor: PerformanceMonitor = FirebasePerformanceMonitor(traceName: Self.traceKey)
    monitor.setMetadata(key: Constants.assetTitleKey, value: assetTitle)
    monitor.start()
    downloadMonitors[mp4URL] = monitor

    let task = downloadSession.makeDownloadTask(with: mp4URL)
    activeDownloads[mp4URL] = task
    assetTitleForURL[mp4URL] = assetTitle
    task.resume()
    print("Started download for \(mp4URL), assetTitle=\(assetTitle)")

    return try await withCheckedThrowingContinuation { continuation in
      downloadContinuations[mp4URL] = continuation
    }
  }

  func prefetch(url: URL, assetTitle: String) async {
    guard assetTitleForURL[url] == nil else { return }
    assetTitleForURL[url] = assetTitle
    inflightAssetForURL[url] = AVURLAsset(url: url)
  }

  func createLocalAssetIfAvailable(for mp4URL: URL) -> AVURLAsset? {
    guard let assetTitle = assetTitleForURL[mp4URL] else { return nil }
    if let url = resolveBookmarkIfPresent(for: assetTitle) {
      downloadedAssetsLRU[assetTitle] = Date()
      return AVURLAsset(url: url)
    }
    return nil
  }

  func localOrInflightAsset(for mp4URL: URL) -> AVURLAsset? {
    if let local = createLocalAssetIfAvailable(for: mp4URL) {
      return local
    }
    return inflightAssetForURL[mp4URL]
  }

  func cancelDownload(for mp4URL: URL) {
    if let task = activeDownloads[mp4URL] {
      task.cancel()
      tidyUpAfterCancellation(url: mp4URL, reason: Constants.performanceCancelKey)
    }
  }

  func elevatePriority(for url: URL) {
    if let task = activeDownloads[url] {
      task.underlyingTask?.priority = URLSessionTask.defaultPriority
    }
  }

  func clearMappingsAndCache(for mp4URL: URL, assetTitle: String) {
    if let localURL = resolveBookmarkIfPresent(for: assetTitle) {
      removeAsset(localURL)
    }
    removeBookmark(for: assetTitle)
    downloadedAssetsLRU.removeValue(forKey: assetTitle)
    delegate?.clearedCache(for: assetTitle)
  }

  private func removeAsset(_ url: URL) {
    Task.detached(priority: .background) { [weak self] in
      guard let self = self else { return }
      do {
        if FileManager.default.fileExists(atPath: url.path) {
          try FileManager.default.removeItem(at: url)
          print("Removed asset at \(url)")
        }
      } catch {
        await crashReporter.recordException(error)
        print("Could not remove asset: \(error)")
      }
    }
  }

  private func enforceCacheLimitIfNeeded() {
    while downloadedAssetsLRU.count >= Constants.maxOfflineAssets {
      if let oldest = downloadedAssetsLRU.min(by: { $0.value < $1.value }) {
        guard let mp4URL = assetTitleForURL.first(where: { $0.value == oldest.key })?.key else { break }
        cancelDownload(for: mp4URL)
        clearMappingsAndCache(for: mp4URL, assetTitle: oldest.key)
      }
    }
  }

  private func tidyUpAfterCancellation(url: URL, reason: String) {
    if let monitor = downloadMonitors[url] {
      monitor.setMetadata(key: Constants.performanceResultKey, value: reason)
      monitor.stop()
      downloadMonitors.removeValue(forKey: url)
    }
    if let continuation = downloadContinuations.removeValue(forKey: url) {
      continuation.resume(throwing: CancellationError())
    }
    activeDownloads.removeValue(forKey: url)
    inflightAssetForURL.removeValue(forKey: url)
  }

  private func storeBookmark(for assetTitle: String, localFileURL: URL) {
    do {
      let data = try localFileURL.bookmarkData()
      storedBookmarks[assetTitle] = data
      userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
      userDefaults.synchronize()
    } catch {
      crashReporter.recordException(error)
      print("Failed to store bookmark for \(assetTitle): \(error)")
    }
  }

  private func removeBookmark(for assetTitle: String) {
    storedBookmarks.removeValue(forKey: assetTitle)
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }

  private func resolveBookmarkIfPresent(for assetTitle: String) -> URL? {
    guard let data = storedBookmarks[assetTitle] else {
      return nil
    }
    var isStale = false
    do {
      let url = try URL(resolvingBookmarkData: data, bookmarkDataIsStale: &isStale)
      if isStale { print("Bookmark stale for \(assetTitle)") }
      return url
    } catch {
      crashReporter.recordException(error)
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
          print("Removed leftover MP4 file: \(url.lastPathComponent)")
        }
      } catch {
        crashReporter.recordException(error)
        print("Failed to remove leftover asset for \(assetTitle): \(error)")
      }
      storedBookmarks.removeValue(forKey: assetTitle)
    }
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }
}

extension MP4DownloadManager: URLSessionDownloadDelegate {
  nonisolated func urlSession(
    _ session: URLSession,
    downloadTask: URLSessionDownloadTask,
    didFinishDownloadingTo location: URL
  ) {
    guard let originalURL = downloadTask.originalRequest?.url else { return }

    let fileManager = FileManager.default
    let caches  = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
    let stagingDir = caches.appendingPathComponent("mp4Staging", isDirectory: true)
    try? fileManager.createDirectory(at: stagingDir, withIntermediateDirectories: true)
    let stagedURL = stagingDir.appendingPathComponent(UUID().uuidString + ".tmp")

    do {
      try fileManager.moveItem(at: location, to: stagedURL)
    } catch {
      print("[MP4DL] staging move failed: \(error)")
      Task { await self.downloadError(for: originalURL, error: error) }
      return
    }
    Task { await self.finishDownload(originalURL: originalURL, tempLocation: stagedURL) }
  }

  nonisolated func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    guard let dlTask = task as? URLSessionDownloadTask,
          let url = dlTask.originalRequest?.url,
          let err = error else { return }
    Task { await self.downloadError(for: url, error: err) }
  }
}

extension MP4DownloadManager {
  func finishDownload(originalURL: URL, tempLocation: URL) async {
    defer { enforceCacheLimitIfNeeded() }

    guard let assetTitle = assetTitleForURL[originalURL] else {
      try? fileManager.removeItem(at: tempLocation)
      tidyUpAfterCancellation(url: originalURL, reason: Constants.performanceErrorKey)
      return
    }

    do {
      let caches = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
      let destDir = caches.appendingPathComponent(Constants.videoKey, isDirectory: true)
      try fileManager.createDirectory(at: destDir, withIntermediateDirectories: true)
      let finalURL = destDir.appendingPathComponent("\(assetTitle).mp4")

      if fileManager.fileExists(atPath: finalURL.path) {
        try fileManager.removeItem(at: finalURL)
      }
      try fileManager.moveItem(at: tempLocation, to: finalURL)

      storeBookmark(for: assetTitle, localFileURL: finalURL)
      downloadedAssetsLRU[assetTitle] = Date()
      delegate?.downloadManager(
        self,
        didFinishAssetFor: originalURL,
        localFileURL: finalURL,
        assetTitle: assetTitle
      )

      if let continuation = downloadContinuations.removeValue(forKey: originalURL) {
        continuation.resume(returning: finalURL)
      }

      if let monitor = downloadMonitors[originalURL] {
        monitor.setMetadata(key: Constants.performanceResultKey, value: Constants.performanceSuccessKey)
        monitor.stop()
        downloadMonitors.removeValue(forKey: originalURL)
      }
    } catch {
      crashReporter.recordException(error)
      tidyUpAfterCancellation(url: originalURL, reason: Constants.performanceErrorKey)
    }

    activeDownloads.removeValue(forKey: originalURL)
    inflightAssetForURL.removeValue(forKey: originalURL)
  }

  func downloadError(for url: URL, error: Error) async {
    tidyUpAfterCancellation(url: url, reason: Constants.performanceErrorKey)
    if let continuation = downloadContinuations.removeValue(forKey: url) { continuation.resume(throwing: error) }
  }
}

protocol MP4DownloadManagerProtocol: AnyObject {
  func clearedCache(for assetTitle: String)
  func downloadManager(
    _ manager: MP4DownloadManaging,
    didFinishAssetFor remoteURL: URL,
    localFileURL: URL,
    assetTitle: String
  )
}

extension MP4DownloadManager {
  enum Constants {
    static let maxOfflineAssets = 10
    static let maxConnectionsPerHost = 3
    static let requestTimeout: TimeInterval = 60
    static let resourceTimeout: TimeInterval = 600

    static let videoKey = "userVideos"
    static let assetTitleKey = "asset_title"
    static let performanceResultKey = "result"
    static let performanceErrorKey = "error"
    static let performanceSuccessKey = "success"
    static let performanceCancelKey = "cancelled"
  }
}
