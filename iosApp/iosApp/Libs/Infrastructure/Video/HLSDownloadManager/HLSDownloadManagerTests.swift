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

  @MainActor override func setUpWithError() throws {
    try super.setUpWithError()
    mockSession = MockAssetDownloadSession()
    mockMonitor = MockNetworkMonitor()
    mockFileManager = MockFileManager()
    delegateSpy = DelegateSpy()

    sut = HLSDownloadManager(
      downloadSession: mockSession,
      networkMonitor: mockMonitor,
      fileManager: mockFileManager
    )
    sut.delegate = delegateSpy
  }

  override func tearDownWithError() throws {
    try super.tearDownWithError()
    sut = nil
    mockSession = nil
    mockMonitor = nil
    mockFileManager = nil
    delegateSpy = nil
  }

  func testStartDownloadAsync_SetsUpAndResumesTask() async throws {
    let testURL = URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest/video.m3u8")! // swiftlint:disable:this line_length
    let testTitle = "TestAsset"
    let finishURL = URL(fileURLWithPath: "/some/local/path")

    let downloadTask = Task {
      return try await sut.startDownloadAsync(hlsURL: testURL, assetTitle: testTitle)
    }
    try await Task.sleep(nanoseconds: 50_000_000) // 50 ms
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
    await Task.yield()
    await sut.cancelDownload(for: testURL)
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
      await MainActor.run {
        sut.assetTitleForURL[url] = "Asset\(item)"
        sut.downloadedAssetsLRU["Asset\(item)"] = Date(timeIntervalSince1970: TimeInterval(item))
        sut.localRemoteUrlMapping[url] = URL(string: "/some/local/path")
      }
    }
    let testURL = URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest/video.m3u8")! // swiftlint:disable:this line_length
    let finishURL = URL(fileURLWithPath: "/some/local/path")
    let downloadTask = Task {
      try await sut.startDownloadAsync(hlsURL: testURL, assetTitle: "Asset random")
    }
    await Task.yield()
    try await Task.sleep(nanoseconds: 50_000_000) // 50 ms
    mockSession.mockTask?.simulateFinish(
      downloadURL: finishURL,
      manager: sut,
      sourceURL: testURL
    )
    let url = try await downloadTask.value
    print(url)
    await MainActor.run {
      XCTAssertNil(sut.downloadedAssetsLRU["Asset0"], "Oldest asset was not removed from the LRU.")
    }
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

  final class DelegateSpy: HLSDownloadManagerProtocol {
    private(set) var clearedCacheCalled = false

    func clearedCache(for assetTitle: String) {
      clearedCacheCalled = true
    }
  }
}
