//
//  FeedsPlayerTests.swift
//  FeedsTests
//
//  Created by Samarth Paboowal on 09/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

@testable import Yral
import XCTest
import AVFoundation

@MainActor
final class FeedsPlayerTests: XCTestCase {

    private func getMockFeeds(count: Int) -> [FeedResult] {
        return (0...count-1).map { index in
            FeedResult(
                postID: "\(index)",
                videoID: "\(index)",
                canisterID: "\(index)",
                principalID: "\(index)",
                // swiftlint:disable:next line_length
                url: Bundle(for: type(of: self)).url(forResource: "video-\(index < 16 ? index : 0)", withExtension: "m3u8")!,
                thumbnail: URL(string: "https://www.google.com")!,
                postDescription: "sample description \(index)",
                likeCount: index,
                isLiked: index % 2 == 1,
                nsfwProbability: 0
            )
        }
    }

    // MARK: - Load Initial Video Tests
    func testLoadInitialVideo_ShouldAddFeeds() throws {
        let feedResults = getMockFeeds(count: 20)
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)

        sut.loadInitialVideos(feedResults)
        XCTAssertEqual(sut.feedResults.count, 20)
        XCTAssertEqual(sut.currentIndex, 0)
    }

    func testLoadInitialVideo_ShouldIncreasePlayCount() async throws {
        let feedResults = getMockFeeds(count: 20)
        let expectation = expectation(description: "Wait for 2 seconds")
        let mockPlayer = MockQueuePlayer {
            expectation.fulfill()
        }
        let sut = FeedsPlayer(player: mockPlayer)

        sut.loadInitialVideos(feedResults)
        XCTAssertEqual(mockPlayer.playCallCount, 0)

        await fulfillment(of: [expectation], timeout: 2)
        XCTAssertEqual(mockPlayer.playCallCount, 1)
    }

    // MARK: - Add Feed Tests
    func testAddFeedResults_ShouldAddAllFeeds() async throws {
        let feedResults = getMockFeeds(count: 20)
        let mockPlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockPlayer)

        sut.loadInitialVideos(Array(feedResults[0...9]))
        sut.addFeedResults(Array(feedResults[10...19]))
        XCTAssertEqual(sut.feedResults.count, 20)
    }

    // MARK: - Advance To Video Tests
    func testAdvanceToVideo_ShouldAdvanceToNextVideo() async throws {
        let feedResults = getMockFeeds(count: 20)
        let expectations = [
            expectation(description: "Wait for 2 seconds"),
            expectation(description: "Wait for 2 seconds")
        ]

        lazy var mockPlayer = MockQueuePlayer {
            if mockPlayer.playCallCount == 1 {
                expectations[0].fulfill()
            } else if mockPlayer.playCallCount == 2 {
                expectations[1].fulfill()
            }
        }

        let sut = FeedsPlayer(player: mockPlayer)

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
            expectation(description: "Wait for 1 deletion"),
            expectation(description: "wait for 10 downloads")
        ]

        let mockPlayer = MockQueuePlayer {}

        let sut = FeedsPlayer(player: mockPlayer)

        sut.onPlayerItemsChanged = { newValue, count in
            let value = newValue ?? -1
            if count == 5 {
                expectations[0].fulfill()
            } else if count == 10 && value < 10 {
                expectations[1].fulfill()
            } else if value == -1 {
                expectations[2].fulfill()
            } else if count == 10 && value >= 10 {
                expectations[3].fulfill()
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
        XCTAssertEqual(sut.playerItems.count, 9)

        await fulfillment(of: [expectations[3]], timeout: 10)
        XCTAssertEqual(sut.playerItems.count, 10)
    }

    func testAdvanceToVideo_ShouldNotAdvanceToNextVideo() throws {
        let feedResults = getMockFeeds(count: 20)
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)
        sut.loadInitialVideos(feedResults)
        sut.advanceToVideo(at: 20)
        XCTAssertEqual(sut.currentIndex, 0)
    }

    // MARK: - Remove Feed Tests
    func testRemoveFeeds_ShouldRemoveAllFeeds() async throws {
        let feedResults = getMockFeeds(count: 20)
        let expectation = expectation(description: "Wait for playerItems to have 5 values")
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)

        sut.onPlayerItemsChanged = { _, count in
            if count == 5 {
                expectation.fulfill()
            }
        }

        sut.loadInitialVideos(Array(feedResults[0...4]))

        await fulfillment(of: [expectation], timeout: 10)
        sut.onPlayerItemsChanged = nil
        XCTAssertEqual(sut.playerItems.count, 5)

        sut.removeFeeds(feedResults)
        XCTAssertEqual(sut.feedResults.count, 0)
        XCTAssertEqual(sut.playerItems.count, 0)
    }

    func testRemoveFeeds_ShouldNotRemoveAllFeeds() async throws {
        let feedResults = getMockFeeds(count: 20)
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)

        let expectation = expectation(description: "Wait for playerItems to have 5 values")

        sut.onPlayerItemsChanged = { _, count in
            if count == 5 {
                expectation.fulfill()
            }
        }

        sut.loadInitialVideos(Array(feedResults[0...4]))

        await fulfillment(of: [expectation], timeout: 10)
        sut.onPlayerItemsChanged = nil
        XCTAssertEqual(sut.playerItems.count, 5)

        sut.removeFeeds(Array(feedResults[0...3]))
        XCTAssertEqual(sut.feedResults.count, 1)
        XCTAssertEqual(sut.playerItems.count, 1)
    }

    func testRemoveFeeds_ShouldNotRemoveAnyFeeds() throws {
        let feedResults = getMockFeeds(count: 20)
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)

        sut.removeFeeds(feedResults)
        XCTAssertEqual(sut.feedResults.count, 0)
        XCTAssertEqual(sut.playerItems.count, 0)
    }

    func testRemoveFeeds_WithEmptyFeeds_ShouldNotRemoveAnyFeeds() throws {
        let feedResults: [FeedResult] = []
        let mockQueuePlayer = MockQueuePlayer {}
        let sut = FeedsPlayer(player: mockQueuePlayer)
        sut.removeFeeds(feedResults)
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

    // swiftlint:disable:next line_length
    func seek(to time: CMTime, toleranceBefore: CMTime, toleranceAfter: CMTime, completionHandler: @escaping (Bool) -> Void) {
        completionHandler(true)
    }
}
