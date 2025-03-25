//
//  FeedsRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import Combine

class FeedsRepository: FeedRepositoryProtocol {
  private let httpService: HTTPService
  private let mlClient: MlFeed_MLFeedNIOClient
  private let authClient: AuthClient
  private let feedsUpdateSubject = PassthroughSubject<[FeedResult], Never>()
  var feedUpdates: AnyPublisher<[FeedResult], Never> { feedsUpdateSubject.eraseToAnyPublisher() }

  init(httpService: HTTPService, mlClient: MlFeed_MLFeedNIOClient, authClient: AuthClient) {
    self.httpService = httpService
    self.mlClient = mlClient
    self.authClient = authClient
  }

  func getInitialFeeds(numResults: Int) async -> Result<Void, FeedError> {
    guard let principal = authClient.canisterPrincipalString else {
      return .failure(FeedError.authError(.authenticationFailed("Missing principal")))
    }
    var cacheResponse: [CacheDTO]
    do {
      cacheResponse = try await httpService.performRequest(
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
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(FeedError.networkError(error))
      default:
        return .failure(FeedError.unknown(error.localizedDescription))
      }
    }
    do {
      _ = try await cacheResponse.asyncMap { feed in
        let mappedFeed = try await self.mapToFeedResults(feed: feed)
        self.feedsUpdateSubject.send([mappedFeed])
      }
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(FeedError.networkError(error))
      case let error as RustError:
        return .failure(FeedError.rustError(error))
      default:
        return .failure(FeedError.unknown(error.localizedDescription))
      }
    }
    self.feedsUpdateSubject.send(completion: .finished)
    return .success(())
  }

  func fetchMoreFeeds(request: MoreFeedsRequest) async -> Result<[FeedResult], FeedError> {
    var mlRequest = MlFeed_FeedRequest()
    mlRequest.canisterID = authClient.canisterPrincipalString ?? ""
    mlRequest.filterPosts = request.filteredPosts
    mlRequest.numResults = UInt32(request.numResults)
    let response: MlFeed_FeedResponse
    do {
      response = try await mlClient.get_feed_clean(
        mlRequest
      ).response.get()
    } catch {
      return .failure(FeedError.networkError(.grpc(error.localizedDescription)))
    }
    do {
      let feeds = try await response.feed.asyncMap { feed in
        let feedResult = try await self.mapToFeedResults(feed: feed)
        return feedResult
      }
      return .success(feeds)
    } catch {
      return .failure(FeedError.rustError(RustError.unknown(error.localizedDescription)))
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
        principalID: result.created_by_user_principal_id().toString(),
        url: videoURL,
        thumbnail: thumbnailURL,
        postDescription: result.description().toString(),
        profileImageURL: urlString != nil ? URL(string: urlString!) : nil,
        likeCount: Int(result.like_count()),
        isLiked: result.liked_by_me()
      )
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
}

extension FeedsRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let cacheBaseURL = "https://yral-ml-feed-cache.go-bazzinga.workers.dev/feed-cache/"

  }
}
