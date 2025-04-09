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
        feedResults = [
            // swiftlint:disable:next line_length
            .init(postID: "1", videoID: "1", canisterID: "1", principalID: "1", url: URL(string: "https://www.google.com")!, thumbnail: URL(string: "https://www.google.com")!, postDescription: "sample description", likeCount: 1, isLiked: true, nsfwProbability: 0),
            // swiftlint:disable:next line_length
            .init(postID: "2", videoID: "2", canisterID: "2", principalID: "2", url: URL(string: "https://www.google.com")!, thumbnail: URL(string: "https://www.google.com")!, postDescription: "sample description", likeCount: 2, isLiked: true, nsfwProbability: 0),
            // swiftlint:disable:next line_length
            .init(postID: "3", videoID: "3", canisterID: "3", principalID: "3", url: URL(string: "https://www.google.com")!, thumbnail: URL(string: "https://www.google.com")!, postDescription: "sample description", likeCount: 3, isLiked: true, nsfwProbability: 0)
        ]
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
