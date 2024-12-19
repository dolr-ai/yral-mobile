//
//  FeedsRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
<<<<<<< HEAD
import Combine
=======
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))

class FeedsRepository: FeedRepositoryProtocol {
  private let httpService: HTTPService
  private let mlClient: MlFeed_MLFeedNIOClient
  private let authClient: AuthClient
<<<<<<< HEAD
  private let feedsUpdateSubject = PassthroughSubject<[FeedResult], Never>()
  var feedUpdates: AnyPublisher<[FeedResult], Never> { feedsUpdateSubject.eraseToAnyPublisher() }
=======
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))

  init(httpService: HTTPService, mlClient: MlFeed_MLFeedNIOClient, authClient: AuthClient) {
    self.httpService = httpService
    self.mlClient = mlClient
    self.authClient = authClient
  }

<<<<<<< HEAD
  func getInitialFeeds(numResults: Int) async throws {
    guard let principal = authClient.principalString else {
      throw AuthError.authenticationFailed("Missing principal")
    }
    var cacheResponse = try await httpService.performRequest(
      for: CacheEndPoints.getUserCanisterCache(
        canisterID: principal,
        numPosts: numResults
      ),
      decodeAs: [CacheDTO].self
    )
    if cacheResponse.isEmpty {
      cacheResponse = try await httpService.performRequest(
        for: CacheEndPoints.getGlobalCache(
          numPosts: numResults
        ),
        decodeAs: [CacheDTO].self
      )
    }
    _ = try await cacheResponse.asyncMap { feed in
      do {
        let feed = try await self.mapToFeedResults(feed: feed)
        self.feedsUpdateSubject.send([feed])
      } catch {
        print(error)
      }
    }
    self.feedsUpdateSubject.send(completion: .finished)
  }

  func fetchMoreFeeds(request: MoreFeedsRequest) async -> Result<[FeedResult], Error> {
    do {
      var mlRequest = MlFeed_FeedRequest()
      mlRequest.canisterID = authClient.principalString ?? ""
      mlRequest.filterPosts = request.filteredPosts
      mlRequest.numResults = UInt32(request.numResults)
      let response = try await mlClient.get_feed_clean(
        mlRequest
      ).response.get()
      let feeds = try await response.feed.asyncMap { feed in
        let feedResult = try await self.mapToFeedResults(feed: feed)
        return feedResult
      }
      return .success(feeds)
    } catch {
      return .failure(error)
    }
  }

  func mapToFeedResults<T: FeedMapping>(feed: T) async throws -> FeedResult {
    let principal = try get_principal(feed.canisterID)
    do {
      let identity = try self.authClient.generateNewDelegatedIdentity()
      let service = try Service(principal, identity)
      let result = try await service.get_individual_post_details_by_id(UInt64(feed.postID))
      let videoURL = URL(
        string: "\(Constants.cloudfarePrefix)\(result.video_uid().toString())\(Constants.cloudflareSuffix)"
      ) ?? URL(fileURLWithPath: "")
      let thumbnailURL = URL(
        string: "\(Constants.cloudfarePrefix)\(result.video_uid().toString())\(Constants.thumbnailSuffix)"
      ) ?? URL(fileURLWithPath: "")
      let urlString = result.created_by_profile_photo_url()?.toString()
      return FeedResult(
        postID: String(feed.postID),
        videoID: result.video_uid().toString(),
        canisterID: feed.canisterID,
        url: videoURL,
        thumbnail: thumbnailURL,
        postDescription: result.description().toString(),
        profileImageURL: urlString != nil ? URL(string: urlString!) : nil,
        likeCount: Int(result.like_count()),
        isLiked: result.liked_by_me()
      )
    }
  }

  func toggleLikeStatus(for request: LikeQuery) async throws -> Result<LikeResult, Error> {
    do {
      let identity = try self.authClient.generateNewDelegatedIdentity()
      let principal = try get_principal(request.canisterID)
      let service = try Service(principal, identity)
      let status = try await service.update_post_toggle_like_status_by_caller(UInt64(request.postID))
      return .success(LikeResult(status: status, index: request.index))
    } catch {
      return .failure(error)
    }
  }
}

class CacheEndPoints {
  static func getUserCanisterCache(canisterID: String, numPosts: Int) -> Endpoint {
    return Endpoint(
      http: "\(canisterID)",
      baseURL: URL(string: FeedsRepository.Constants.cacheBaseURL)!,
      path: "\(canisterID)",
      method: .get,
      queryItems: [
        URLQueryItem(name: "start", value: "\(Int.zero)"),
        URLQueryItem(name: "limit", value: String(numPosts))
      ],
      headers: ["Content-Type": "application/json"]
    )
  }

  static func getGlobalCache(numPosts: Int) -> Endpoint {
    return Endpoint(
      http: "global-feed",
      baseURL: URL(string: FeedsRepository.Constants.cacheBaseURL)!,
      path: "global-feed",
      method: .get,
      queryItems: [
        URLQueryItem(name: "start", value: "\(Int.zero)"),
        URLQueryItem(name: "limit", value: String(numPosts))
      ],
      headers: ["Content-Type": "application/json"]
    )
  }
}

protocol FeedMapping {
  var postID: UInt32 { get }
  var canisterID: String { get }
=======
  func fetchFeed(request: FeedRequest) async -> Result<[FeedResult], Error> {
    var mlRequest = MlFeed_FeedRequest()
    mlRequest.canisterID = authClient.principalString ?? ""
    mlRequest.filterPosts = request.filteredPosts
    mlRequest.numResults = UInt32(request.numResults)
    do {
      let response = try await mlClient.get_feed_clean(
        mlRequest
      ).response.get()
      let feeds = try await response.feed.asyncMap { feed in
        let principal = try get_principal(feed.canisterID)
        do {
          let identity = try self.authClient.generateNewDelegatedIdentity()
          let service = try Service(principal, identity)
          let result = try await service.get_individual_post_details_by_id(UInt64(feed.postID))
          let videoURL = URL(
            string: "\(Constants.cloudfarePrefix)\(result.video_uid().toString())\(Constants.cloudflareSuffix)"
          ) ?? URL(fileURLWithPath: "")
          return FeedResult(id: String(feed.postID), url: videoURL)
        }
      }
      return .success(feeds)
    } catch {
      return .failure(error)
    }
  }
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
}

extension FeedsRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
<<<<<<< HEAD
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let cacheBaseURL = "https://yral-ml-feed-cache.go-bazzinga.workers.dev/feed-cache/"

=======
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
  }
}
