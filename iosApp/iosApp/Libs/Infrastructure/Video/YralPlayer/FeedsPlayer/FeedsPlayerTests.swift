//
//  FeedsPlayerTests.swift
//  FeedsTests
//
//  Created by Samarth Paboowal on 09/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

@testable import Yral
import XCTest

@MainActor
final class FeedsPlayerTests: XCTestCase {

    private var sut: FeedsPlayer!
    private var feedResults: [FeedResult] = []

    override func setUpWithError() throws {
        sut = FeedsPlayer()
        feedResults = (1...15).map { index in
            FeedResult(
                postID: "\(index)",
                videoID: "\(index)",
                canisterID: "\(index)",
                principalID: "\(index)",
                url: URL(string: "https://www.google.com")!,
                thumbnail: URL(string: "https://www.google.com")!,
                postDescription: "sample description \(index)",
                likeCount: index,
                isLiked: index % 2 == 1,
                nsfwProbability: 0
            )
        }
    }

    override func tearDownWithError() throws {
        sut = nil
        feedResults = []
    }

    // MARK: - Load Initial Video Tests
    func testLoadInitialVideo_ShouldAddFeeds() throws {
        sut.loadInitialVideos(feedResults)
        XCTAssertEqual(sut.feedResults.count, 3)
        XCTAssertEqual(sut.currentIndex, 0)
    }

    // MARK: - Add Feed Tests
    func testAddFeedResults_ShouldAddAllFeeds() throws {
        sut.addFeedResults(feedResults)
        XCTAssertEqual(sut.feedResults.count, 3)
    }

    // MARK: - Advance To Video Tests
    func testAdvanceToVideo_ShouldAdvanceToNextVideo() throws {
        sut.feedResults = feedResults
        let index = 1
        sut.advanceToVideo(at: index)
        XCTAssertEqual(sut.currentIndex, 1)
    }

    func testAdvanceToVideo_ShouldNotAdvanceToNextVideo() throws {
        sut.feedResults = feedResults
        let index = 3
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
        sut.removeFeeds(Array(feedResults[0...1]))
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
