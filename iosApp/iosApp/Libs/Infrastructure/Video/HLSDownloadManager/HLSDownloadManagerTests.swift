//
//  HLSDownloadManagerTests.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 09/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

@testable import Yral
import XCTest
import AVFoundation
import Network

final class HLSDownloadManagerTests: XCTestCase {

  private var sut: HLSDownloadManager!
  private var mockSession: MockAssetDownloadSession!
  private var mockMonitor: MockNetworkMonitor!
  private var mockFileManager: MockFileManager!
  private var delegateSpy: DelegateSpy!

  @MainActor override func setUp() async throws {
    try await super.setUp()
    mockSession = MockAssetDownloadSession()
    mockMonitor = MockNetworkMonitor()
    mockFileManager = MockFileManager()
    delegateSpy = DelegateSpy()

    sut = HLSDownloadManager(
      downloadSession: mockSession,
      networkMonitor: mockMonitor,
      fileManager: mockFileManager,
      crashReporter: MockCrashReporter()
    )
    await sut.setDelegate(delegateSpy)
  }

  override func tearDownWithError() throws {
    try super.tearDownWithError()
    sut = nil
    mockSession = nil
    mockMonitor = nil
    mockFileManager = nil
    delegateSpy = nil
  }

  /// Polls until `startDownloadAsync` has stored its continuation on the actor, with a
  /// 10-second deadline. Using a fixed `Task.sleep` is unreliable on slow CI machines
  /// because the actor hop can take longer than the fixed delay, leaving `simulateFinish`
  /// or `cancelDownload` running before the continuation is registered.
  private func waitForContinuation(url: URL) async throws {
    let deadline = Date().addingTimeInterval(10)
    while await !sut.hasContinuation(for: url) {
      guard Date() < deadline else {
        XCTFail("startDownloadAsync did not register its continuation within 10 seconds")
        return
      }
      try await Task.sleep(nanoseconds: 10_000_000) // 10 ms poll
    }
  }

  func testStartDownloadAsync_SetsUpAndResumesTask() async throws {
    let testURL = URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest/video.m3u8")! // swiftlint:disable:this line_length
    let testTitle = "TestAsset"
    let finishURL = URL(fileURLWithPath: "/some/local/path")

    let downloadTask = Task {
      return try await sut.startDownloadAsync(hlsURL: testURL, assetTitle: testTitle)
    }
    // Poll until startDownloadAsync has registered its continuation on the actor.
    // A fixed sleep is not reliable on slow CI machines — the actor hop can take longer
    // than 50 ms, causing simulateFinish to run before the continuation is stored,
    // which would leave the continuation un-resumed and hang the test.
    try await waitForContinuation(url: testURL)
    mockSession.mockTask?.simulateFinish(
      downloadURL: finishURL,
      manager: sut,
      sourceURL: testURL
    )

    let returnedURL = try await downloadTask.value

    XCTAssertEqual(mockSession.mockTask?.resumeCallCount, 1, "Task must be resumed exactly once.")
    XCTAssertEqual(returnedURL.path, finishURL.path, "The returned URL should match the finished URL.")
    XCTAssertTrue(mockSession.makeAssetDownloadTaskCalled, "Should create a download task.")
  }

  func testCancelDownload_RemovesTaskAndContinuation() async throws {
    let testURL = URL(string: "http://example.com/video.m3u8")!
    let finishURL = URL(fileURLWithPath: "/some/local/path")
    let downloadTask = Task { () -> URL in
      return try await sut.startDownloadAsync(
        hlsURL: testURL,
        assetTitle: "SomeTitle"
      )
    }
    // Poll until startDownloadAsync has registered its continuation on the actor.
    // cancelDownload must run after the continuation exists so it can resume it with
    // CancellationError; otherwise cancelDownload is a no-op and simulateFinish later
    // completes the continuation leaving the test assertion wrong or hanging forever.
    try await waitForContinuation(url: testURL)
    await sut.cancelDownload(for: testURL)
    // Give the cancellation Tasks a moment to propagate before simulateFinish
    try await Task.sleep(nanoseconds: 50_000_000)
    mockSession.mockTask?.simulateFinish(
      downloadURL: finishURL,
      manager: sut,
      sourceURL: testURL
    )
    do {
      _ = try await downloadTask.value
      XCTFail("Expected the download not to finish successfully after cancellation!")
    } catch {
      print(error)
    }
    XCTAssertEqual(mockSession.mockTask?.cancelCallCount, 1, "Expected cancel() to be called exactly once.")
  }

  func testEnforceCacheLimitIfNeeded_RemovesOldestWhenLimitExceeded() async throws {
    let max = HLSDownloadManager.Constants.maxOfflineAssets
    for item in 0..<(max + 1) {
      let url = URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest\(item)/video.m3u8")! // swiftlint:disable:this line_length
      await sut.setupTestEntry(
        url: url,
        assetTitle: "Asset\(item)",
        date: Date(timeIntervalSince1970: TimeInterval(item))
      )
    }
    let testURL = URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest/video.m3u8")! // swiftlint:disable:this line_length
    let finishURL = URL(fileURLWithPath: "/some/local/path")
    let downloadTask = Task {
      try await sut.startDownloadAsync(hlsURL: testURL, assetTitle: "Asset random")
    }
    // Same polling strategy: wait for the continuation to be registered before
    // calling simulateFinish so the resume actually reaches it.
    try await waitForContinuation(url: testURL)
    mockSession.mockTask?.simulateFinish(
      downloadURL: finishURL,
      manager: sut,
      sourceURL: testURL
    )
    let url = try await downloadTask.value
    print(url)
    let lruDate = await sut.downloadedLRUDate(for: "Asset0")
    XCTAssertNil(lruDate, "Oldest asset was not removed from the LRU.")
  }

  final class MockAssetDownloadSession: AVAssetDownloadURLSessionProtocol {
    var makeAssetDownloadTaskCalled = false
    var mockTask: MockAssetDownloadTask? = MockAssetDownloadTask()

    func makeAssetDownloadTask(downloadConfiguration: AVAssetDownloadConfiguration) -> AVAssetDownloadTaskProtocol? {
      makeAssetDownloadTaskCalled = true
      return mockTask
    }
  }

  final class MockTaskReference: NSObject, @unchecked Sendable { }
  final class MockAssetDownloadTask: AVAssetDownloadTaskProtocol {
    var resumeCallCount = 0
    var cancelCallCount = 0
    private let fakeReference = MockTaskReference()
    var underlyingTask: AVAssetDownloadTask? {
      unsafeBitCast(fakeReference, to: AVAssetDownloadTask.self)
    }

    func resume() {
      resumeCallCount += 1
    }

    func cancel() {
      cancelCallCount += 1
    }
    func simulateFinish(downloadURL: URL, manager: HLSDownloadManager, sourceURL: URL) {
      let castedTask = unsafeBitCast(fakeReference, to: AVAssetDownloadTask.self)
      Task.detached {
        manager.urlSession(
          URLSession.shared,
          assetDownloadTask: castedTask,
          willDownloadTo: downloadURL
        )
        manager.urlSession(
          URLSession.shared,
          assetDownloadTask: castedTask,
          didFinishDownloadingTo: downloadURL
        )
      }
    }
  }

  final class MockNetworkMonitor: NetworkMonitorProtocol {
    var isGoodForPrefetch: Bool = true
    var isNetworkAvailable: Bool = true
    func startMonitoring() {
    }
  }

  final class MockFileManager: FileManager {
    var removeItemCalled = false

    override func removeItem(at URL: URL) throws {
      removeItemCalled = true
    }
  }

  final class MockCrashReporter: CrashReporter {
    func setUserId(_ userId: String) {}
    func recordException(_ error: Error) {}
    func log(_ message: String) {}
    func setMetadata(key: String, value: String) {}
  }

  final class DelegateSpy: HLSDownloadManagerProtocol {
    private(set) var clearedCacheCalled = false

    func clearedCache(for assetTitle: String) {
      clearedCacheCalled = true
    }

    func downloadManager(
      _ manager: any HLSDownloadManaging,
      didFinishAssetFor remoteURL: URL,
      localFileURL: URL,
      assetTitle: String
    ) {}

    func downloadManager(
      _ manager: any HLSDownloadManaging,
      didBeginAssetFor remoteURL: URL,
      tempDirURL: URL,
      assetTitle: String
    ) {}
  }
}

// MARK: - Test helpers
extension HLSDownloadManager {
  func setupTestEntry(url: URL, assetTitle: String, date: Date) {
    assetTitleForURL[url] = assetTitle
    downloadedAssetsLRU[assetTitle] = date
  }

  func downloadedLRUDate(for title: String) -> Date? {
    downloadedAssetsLRU[title]
  }

  func hasContinuation(for url: URL) -> Bool {
    downloadContinuations[url] != nil
  }
}
