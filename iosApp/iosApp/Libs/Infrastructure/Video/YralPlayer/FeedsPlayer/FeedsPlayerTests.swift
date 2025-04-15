@testable import Yral
import XCTest
import AVFoundation

@MainActor
final class FeedsPlayerTests: XCTestCase {

  private func getMockFeeds(count: Int) -> [FeedResult] {
    return (0..<count).map { index in
      let url = Bundle(for: type(of: self))
        .url(forResource: "video-\(index < 16 ? index : 0)", withExtension: "m3u8")!

      return FeedResult(
        postID: "\(index)",
        videoID: "\(index)",
        canisterID: "\(index)",
        principalID: "\(index)",
        url: url,
        thumbnail: URL(string: "https://www.google.com/\(index).jpg")!,
        postDescription: "sample description \(index)",
        likeCount: index,
        isLiked: (index % 2 == 1),
        nsfwProbability: 0
      )
    }
  }

  func testLoadInitialVideo_ShouldAddFeeds() throws {
    let feedResults = getMockFeeds(count: 20)
    let mockQueuePlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()
    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockQueuePlayer, hlsDownloadManager: mockDownloadManager)

    sut.loadInitialVideos(feedResults)

    XCTAssertEqual(sut.feedResults.count, 20)
    XCTAssertEqual(sut.currentIndex, 0)
  }

  func testLoadInitialVideo_ShouldIncreasePlayCount() async throws {
    let feedResults = getMockFeeds(count: 20)
    let expectation = expectation(description: "Wait for play() call")

    let mockPlayer = MockQueuePlayer {
      expectation.fulfill()
    }
    let mockDownloadManager = MockHLSDownloadManager()
    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    sut.loadInitialVideos(feedResults)
    XCTAssertEqual(mockPlayer.playCallCount, 0)

    await fulfillment(of: [expectation], timeout: 2)
    XCTAssertEqual(mockPlayer.playCallCount, 1)
  }

  func testAddFeedResults_ShouldAddAllFeeds() async throws {
    let feedResults = getMockFeeds(count: 20)
    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    let initialFeeds = Array(feedResults[0...9])
    let additionalFeeds = Array(feedResults[10...19])

    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    sut.loadInitialVideos(initialFeeds)
    sut.addFeedResults(additionalFeeds)
    XCTAssertEqual(sut.feedResults.count, 20)
  }

  func testAdvanceToVideo_ShouldAdvanceToNextVideo() async throws {
    let feedResults = getMockFeeds(count: 20)

    let expectations = [
      expectation(description: "First play() call"),
      expectation(description: "Second play() call")
    ]

    lazy var mockPlayer = MockQueuePlayer {
      if mockPlayer.playCallCount == 1 {
        expectations[0].fulfill()
      } else if mockPlayer.playCallCount == 2 {
        expectations[1].fulfill()
      }
    }

    let mockDownloadManager = MockHLSDownloadManager()
    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)
    sut.loadInitialVideos(feedResults)

    await fulfillment(of: [expectations[0]], timeout: 2)
    XCTAssertEqual(mockPlayer.playCallCount, 1)

    sut.advanceToVideo(at: 1)
    XCTAssertEqual(sut.currentIndex, 1)

    await fulfillment(of: [expectations[1]], timeout: 2)
    XCTAssertEqual(mockPlayer.playCallCount, 2)
  }

  func testAdvanceToVideo_ShouldRemoveOfflineAssets() async throws {
    let feedResults = getMockFeeds(count: 20)
    let expectations = [
      expectation(description: "Wait for 5 downloads"),
      expectation(description: "Wait for 10 downloads"),
      expectation(description: "Wait for 1 deletion")
    ]

    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager(maxOfflineAssets: 8)
    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    var flag = false

    sut.onPlayerItemsChanged = { newValue, count in
      let value = newValue ?? -1
      if count == 5 {
        expectations[0].fulfill()
      } else if count == 10 && value < 10 {
        flag = true
        expectations[1].fulfill()
      } else if count == 10 && flag {
        expectations[2].fulfill()
      }
    }

    sut.loadInitialVideos(feedResults)

    await fulfillment(of: [expectations[0]], timeout: 10)
    XCTAssertEqual(sut.playerItems.count, 5)

    sut.advanceToVideo(at: 5)
    await fulfillment(of: [expectations[1]], timeout: 10)
    XCTAssertEqual(sut.playerItems.count, 10)

    sut.advanceToVideo(at: 6)
    await fulfillment(of: [expectations[2]], timeout: 10)
    XCTAssertEqual(sut.playerItems.count, 10)
  }

  func testAdvanceToVideo_ShouldNotAdvanceToNextVideo() throws {
    let feedResults = getMockFeeds(count: 20)
    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    for feed in feedResults {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)
    sut.loadInitialVideos(feedResults)

    sut.advanceToVideo(at: 20)
    XCTAssertEqual(sut.currentIndex, 0)
  }

  func testRemoveFeeds_ShouldRemoveAllFeeds() async throws {
    let feedResults = getMockFeeds(count: 20)
    let expectation = expectation(description: "Wait for playerItems to have 5 values")

    let mockQueuePlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    let subset = Array(feedResults[0...4])
    for feed in subset {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockQueuePlayer, hlsDownloadManager: mockDownloadManager)

    sut.onPlayerItemsChanged = { _, count in
      if count == 5 {
        expectation.fulfill()
      }
    }

    sut.loadInitialVideos(subset)

    await fulfillment(of: [expectation], timeout: 10)
    XCTAssertEqual(sut.playerItems.count, 5)

    sut.removeFeeds(subset)
    XCTAssertEqual(sut.feedResults.count, 0)
    XCTAssertEqual(sut.playerItems.count, 0)
  }

  func testRemoveFeeds_ShouldNotRemoveAllFeeds() async throws {
    let feedResults = getMockFeeds(count: 20)
    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    let subset = Array(feedResults[5...9])
    for feed in subset {
      mockDownloadManager.localURLsForTest[feed.url] = feed.url
    }

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    let expectation = expectation(description: "Wait for 5 items")

    sut.onPlayerItemsChanged = { _, count in
      if count == 5 {
        expectation.fulfill()
      }
    }

    sut.loadInitialVideos(subset)

    await fulfillment(of: [expectation], timeout: 10)
    XCTAssertEqual(sut.playerItems.count, 5)

    sut.removeFeeds(Array(subset[0...3]))
    XCTAssertEqual(sut.feedResults.count, 1)
    XCTAssertEqual(sut.playerItems.count, 1)
  }

  func testRemoveFeeds_ShouldNotRemoveAnyFeeds() throws {
    let feedResults = getMockFeeds(count: 20)
    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    sut.removeFeeds(feedResults)
    XCTAssertEqual(sut.feedResults.count, 0)
    XCTAssertEqual(sut.playerItems.count, 0)
  }

  func testRemoveFeeds_WithEmptyFeeds_ShouldNotRemoveAnyFeeds() throws {
    let emptyFeedResults: [FeedResult] = []
    let mockPlayer = MockQueuePlayer {}
    let mockDownloadManager = MockHLSDownloadManager()

    let sut = FeedsPlayer(player: mockPlayer, hlsDownloadManager: mockDownloadManager)

    sut.removeFeeds(emptyFeedResults)
    XCTAssertEqual(sut.feedResults.count, 0)
    XCTAssertEqual(sut.playerItems.count, 0)
  }
}

final class MockQueuePlayer: YralQueuePlayer {
  var isMuted = false
  var currentItem: AVPlayerItem?
  var onPlay: () -> Void

  private(set) var playCallCount = 0
  private(set) var pauseCallCount = 0
  private(set) var removeAllItemsCallCount = 0

  init(onPlay: @escaping () -> Void) {
    self.onPlay = onPlay
  }

  func play() {
    playCallCount += 1
    onPlay()
  }

  func pause() {
    pauseCallCount += 1
  }

  func removeAllItems() {
    removeAllItemsCallCount += 1
    currentItem = nil
  }

  func currentTime() -> CMTime {
    CMTime(value: 0, timescale: 1)
  }

  func seek(
    to time: CMTime,
    toleranceBefore: CMTime,
    toleranceAfter: CMTime,
    completionHandler: @escaping (Bool) -> Void
  ) {
    completionHandler(true)
  }
}

@MainActor
final class MockHLSDownloadManager: HLSDownloadManaging {
  weak var delegate: HLSDownloadManagerProtocol?

  var localURLsForTest: [URL: URL] = [:]
  var downloadedAssetsLRU: [String: Date] = [:]
  var assetTitleForURL: [URL: String] = [:]
  private var maxOfflineAssets: Int

  init(maxOfflineAssets: Int = 10) {
    self.maxOfflineAssets = maxOfflineAssets
  }
  func startDownloadAsync(hlsURL: URL, assetTitle: String) async throws -> URL {
    guard let localURL = localURLsForTest[hlsURL] else {
      throw NSError(
        domain: "MockHLSDownloadManager",
        code: -1,
        userInfo: [NSLocalizedDescriptionKey: "No localURL mapped for \(hlsURL) in MockHLSDownloadManager"]
      )
    }

    assetTitleForURL[hlsURL] = assetTitle
    downloadedAssetsLRU[assetTitle] = Date()

    enforceCacheLimitIfNeeded()

    return localURL
  }

  func createLocalAssetIfAvailable(for hlsURL: URL) -> AVURLAsset? {
    guard let localURL = localURLsForTest[hlsURL] else {
      return nil
    }
    return AVURLAsset(url: localURL)
  }

  func cancelDownload(for hls: URL) {
  }

  func clearMappingsAndCache(for hls: URL, assetTitle: String) {
    localURLsForTest.removeValue(forKey: hls)
    downloadedAssetsLRU.removeValue(forKey: assetTitle)
    delegate?.clearedCache(for: assetTitle)
  }

  private func enforceCacheLimitIfNeeded() {
    while downloadedAssetsLRU.count > maxOfflineAssets {
      let sorted = downloadedAssetsLRU.sorted { $0.value < $1.value }
      guard let oldest = sorted.first else { break }

      guard let hlsURL = assetTitleForURL.first(where: { $0.value == oldest.key })?.key else {
        downloadedAssetsLRU.removeValue(forKey: oldest.key)
        continue
      }
      clearMappingsAndCache(for: hlsURL, assetTitle: oldest.key)
    }
  }
}
