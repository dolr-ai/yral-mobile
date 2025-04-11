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

    private var sut: FeedsPlayer!
    private var mockQueuePlayer: MockQueuePlayer!
    private var feedResults: [FeedResult] = []

    override func setUpWithError() throws {
        mockQueuePlayer = MockQueuePlayer {}
        sut = FeedsPlayer(player: mockQueuePlayer)
        feedResults = (0...19).map { index in
            FeedResult(
                postID: "\(index)",
                videoID: "\(index)",
                canisterID: "\(index)",
                principalID: "\(index)",
                // swiftlint:disable:next line_length
                url: URL(string: "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/86990fde9b46455d8b191a9019e89e96/manifest/video.m3u8")!,
                thumbnail: URL(string: "https://www.google.com")!,
                postDescription: "sample description \(index)",
                likeCount: index,
                isLiked: index % 2 == 1,
                nsfwProbability: 0
            )
        }
    }

    override func tearDownWithError() throws {
        mockQueuePlayer = nil
        sut = nil
        feedResults = []
    }

    // MARK: - Load Initial Video Tests
    func testLoadInitialVideo_ShouldAddFeeds() throws {
        sut.loadInitialVideos(feedResults)
        XCTAssertEqual(sut.feedResults.count, 20)
        XCTAssertEqual(sut.currentIndex, 0)
    }

    func testLoadInitialVideo_ShouldIncreasePlayCount() async throws {
        let expectation = expectation(description: "Wait for 2 seconds")
        let mockPlayer = MockQueuePlayer {
            expectation.fulfill()
        }
        let sut = FeedsPlayer(player: mockPlayer)

        sut.loadInitialVideos(feedResults)
        XCTAssertEqual(mockQueuePlayer.playCallCount, 0)

        await fulfillment(of: [expectation])
        XCTAssertEqual(mockPlayer.playCallCount, 1)
    }

    // MARK: - Add Feed Tests
    func testAddFeedResults_ShouldAddAllFeeds() async throws {
        sut.addFeedResults(feedResults)
        XCTAssertEqual(sut.feedResults.count, 20)
    }

    // MARK: - Advance To Video Tests
    func testAdvanceToVideo_ShouldAdvanceToNextVideo() async throws {
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

        let index = 1
        sut.advanceToVideo(at: index)
        XCTAssertEqual(sut.currentIndex, 1)

        await fulfillment(of: [expectations[1]], timeout: 2)
        XCTAssertEqual(mockPlayer.playCallCount, 2)
    }

    func testAdvanceToVideo_ShouldRemoveOfflineAssets() async throws {
        let expectations = [
            expectation(description: "Wait for 5 downloads"),
            expectation(description: "Wait for 10 downloads"),
            expectation(description: "Wait for 15 downloads")
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
            }
        }

        sut.loadInitialVideos(feedResults)

        await fulfillment(of: [expectations[0]], timeout: 5)
        XCTAssertEqual(sut.playerItems.count, 5)

        sut.advanceToVideo(at: 5)

        await fulfillment(of: [expectations[1]], timeout: 5)
        XCTAssertEqual(sut.playerItems.count, 10)

        sut.advanceToVideo(at: 15)
        await fulfillment(of: [expectations[2]], timeout: 5)
        XCTAssertEqual(sut.playerItems.count, 9)
    }

    func testAdvanceToVideo_ShouldNotAdvanceToNextVideo() throws {
        sut.feedResults = feedResults
        let index = 20
        sut.advanceToVideo(at: index)
        XCTAssertEqual(sut.currentIndex, 0)
    }

    // MARK: - Remove Feed Tests
    func testRemoveFeeds_ShouldRemoveAllFeeds() throws {
        sut.feedResults = feedResults
        sut.removeFeeds(feedResults)
        XCTAssertEqual(sut.feedResults.count, 0)
    }

    func testRemoveFeeds_ShouldNotRemoveAllFeeds() throws {
        sut.feedResults = feedResults
        sut.removeFeeds(Array(feedResults[0...18]))
        XCTAssertEqual(sut.self.feedResults.count, 1)
    }

    func testRemoveFeeds_ShouldNotRemoveAnyFeeds() throws {
        sut.removeFeeds(feedResults)
        XCTAssertEqual(sut.feedResults.count, 0)
    }

    func testRemoveFeeds_WithEmptyFeeds_ShouldNotRemoveAnyFeeds() throws {
        feedResults = []
        sut.removeFeeds(feedResults)
        XCTAssertEqual(sut.feedResults.count, 0)
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
