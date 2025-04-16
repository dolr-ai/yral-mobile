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
        for: CacheEndPoints.getGlobalCache(
          request: FeedRequestDTO(
          canisterID: principal,
          filterResults: [],
          numResults: Constants.initialNumResults
        )),
        decodeAs: PostsResponse.self
      ).posts
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(FeedError.networkError(error))
      default:
        return .failure(FeedError.unknown(error.localizedDescription))
      }
    }

    var aggregatedErrors: [Error] = []

    for feed in cacheResponse {
      do {
        let mappedFeed = try await self.mapToFeedResults(feed: feed)
        self.feedsUpdateSubject.send([mappedFeed])
      } catch {
        let feedError = self.handleFeedError(error: error)
        aggregatedErrors.append(feedError)
      }
    }

    if !aggregatedErrors.isEmpty {
      return .failure(FeedError.aggregated(AggregatedError(errors: aggregatedErrors)))
    }

    return .success(())
  }

  func fetchMoreFeeds(request: MoreFeedsRequest) async -> Result<[FeedResult], FeedError> {
    guard let principal = authClient.canisterPrincipalString else {
      return .failure(FeedError.authError(.authenticationFailed("Missing principal")))
    }
    var feedResponse: [CacheDTO]
    do {
      let filteredPosts = request.filteredPosts.map { $0.asFilteredResultDTO() }
      feedResponse = try await httpService.performRequest(
        for: CacheEndPoints.getMLFeed(
          request: FeedRequestDTO(
          canisterID: principal,
          filterResults: filteredPosts,
          numResults: Constants.mlNumResults
        )),
        decodeAs: PostsResponse.self
      ).posts
    } catch {
      return .failure(FeedError.networkError(.invalidResponse(error.localizedDescription)))
    }

    var aggregatedErrors: [Error] = []
    var result: [FeedResult] = []

    for feed in feedResponse {
      do {
        let feedResult = try await self.mapToFeedResults(feed: feed)
        result.append(feedResult)
        self.feedsUpdateSubject.send([feedResult])
      } catch {
        let feedError = self.handleFeedError(error: error)
        aggregatedErrors.append(feedError)
      }
    }

    if !aggregatedErrors.isEmpty {
      return .failure(FeedError.aggregated(AggregatedError(errors: aggregatedErrors)))
    }
    return .success(result)
  }

  func mapToFeedResults<T: FeedMapping>(feed: T) async throws -> FeedResult {
    let principal = try get_principal(feed.canisterID)
    let identity = try self.authClient.generateNewDelegatedIdentity()
    let service = try Service(principal, identity)
    let result = try await service.get_individual_post_details_by_id(UInt64(feed.postID))
    guard result.status().is_banned_due_to_user_reporting() == false else {
      throw FeedError.rustError(RustError.unknown("Post is banned"))
    }
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
      isLiked: result.liked_by_me(),
      nsfwProbability: feed.nsfwProbability
    )
  }

  private func handleFeedError(error: Error) -> FeedError {
    switch error {
    case let error as NetworkError:
      return FeedError.networkError(error)
    case let error as RustError:
      return FeedError.rustError(error)
    default:
      return FeedError.unknown(error.localizedDescription)
    }
  }

  func reportVideo(request: ReportRequest) async -> Result<String, FeedError> {
    guard let canisterPrincipalString = authClient.canisterPrincipalString,
          let userPrincipalString = authClient.userPrincipalString else {
      return .failure(FeedError.authError(AuthError.authenticationFailed("No canister principal")))
    }
    guard let baseURL = httpService.baseURL else { return .failure(.networkError(NetworkError.invalidRequest)) }
    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let reportRequestDTO = ReportRequestDTO(
        canisterId: request.canisterID,
        principal: request.principal,
        postId: request.postId,
        reason: request.reason,
        userCanisterId: canisterPrincipalString,
        userPrincipal: userPrincipalString,
        videoId: request.videoId,
        delegatedIdentityWire: swiftWire
      )
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.reportVideoPath,
        method: .post,
        headers: ["Content-Type": "application/json"],
        body: try JSONEncoder().encode(reportRequestDTO)
      )
      _ = try await httpService.performRequest(for: endpoint)
      return .success((String(request.postId)))
    } catch {
      switch error {
      case let netErr as NetworkError:
        return .failure(FeedError.networkError(netErr))
      case let authErr as AuthError:
        return .failure(FeedError.authError(authErr))
      default:
        return .failure(.unknown(error.localizedDescription))
      }
    }
  }

  private func swiftDelegatedIdentityWire(from rustWire: DelegatedIdentityWire) throws -> SwiftDelegatedIdentityWire {
    let wireJsonString = delegated_identity_wire_to_json(rustWire).toString()
    guard let data = wireJsonString.data(using: .utf8) else {
      throw AuthError.authenticationFailed("Failed to convert wire JSON string to Data")
    }
    return try JSONDecoder().decode(SwiftDelegatedIdentityWire.self, from: data)
  }
}

class CacheEndPoints {
  static func getGlobalCache(request: FeedRequestDTO) throws -> Endpoint {
    return Endpoint(
      http: "global-feed",
      baseURL: URL(string: FeedsRepository.Constants.feedsBaseURL)!,
      path: FeedsRepository.Constants.cacheSuffix,
      method: .post,
      headers: ["Content-Type": "application/json"],
      body: try JSONEncoder().encode(request)
    )
  }

  static func getMLFeed(request: FeedRequestDTO) throws -> Endpoint {
    return Endpoint(
      http: "global-feed",
      baseURL: URL(string: FeedsRepository.Constants.feedsBaseURL)!,
      path: FeedsRepository.Constants.mlFeedSuffix,
      method: .post,
      headers: ["Content-Type": "application/json"],
      body: try JSONEncoder().encode(request)
    )
  }
}

protocol FeedMapping {
  var postID: UInt32 { get }
  var canisterID: String { get }
  var nsfwProbability: Double { get }
}

extension FeedsRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let feedsBaseURL = "https://yral-ml-feed-server.fly.dev"
    static let cacheSuffix = "/api/v1/feed/coldstart/clean"
    static let mlFeedSuffix = "/api/v1/feed/clean"
    static let reportVideoPath = "/api/v1/posts/report"
    static let initialNumResults: Int64 = 20
    static let mlNumResults: Int64 = 10
  }
}
