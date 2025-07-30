//
//  FeedsRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Combine

// swiftlint: disable type_body_length
class FeedsRepository: FeedRepositoryProtocol {
  private let httpService: HTTPService
  private let firebaseService: FirebaseServiceProtocol
  private let mlClient: MlFeed_MLFeedNIOClient
  private let authClient: AuthClient
  private let feedsUpdateSubject = PassthroughSubject<[FeedResult], Never>()
  var feedUpdates: AnyPublisher<[FeedResult], Never> { feedsUpdateSubject.eraseToAnyPublisher() }

  init(
    httpService: HTTPService,
    firebaseService: FirebaseServiceProtocol,
    mlClient: MlFeed_MLFeedNIOClient,
    authClient: AuthClient
  ) {
    self.httpService = httpService
    self.firebaseService = firebaseService
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
            userID: principal,
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
      return .failure(
        FeedError.aggregated(
          AggregatedError(
            errors: aggregatedErrors
          ),
          nil
        )
      )
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
            userID: principal,
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
      return .failure(
        FeedError.aggregated(
          AggregatedError(
            errors: aggregatedErrors
          ),
          result
        )
      )
    }
    return .success(result)
  }

  private func mapToFeedResults<T: FeedMapping>(feed: T) async throws -> FeedResult {
    let principal = try get_principal(feed.canisterID)
    let identity = try self.authClient.generateNewDelegatedIdentity()
    let service = try Service(principal, identity)
    let result = try await service.get_individual_post_details_by_id(UInt64(feed.postID))
    let voteExistsForSmileyGame = (try? await fetchSmileyGamePlayedStatus(for: result.video_uid().toString())) ?? false

    guard result.status().is_banned_due_to_user_reporting() == false else {
      throw FeedError.rustError(RustError.unknown("Post is banned"))
    }

    guard !voteExistsForSmileyGame else {
      throw FeedError.firebaseError("User has already voted for this video")
    }

    let videoURL = URL(
      string: "\(Constants.cloudfarePrefix)\(result.video_uid().toString())\(Constants.cloudflareSuffix)"
    ) ?? URL(fileURLWithPath: "")

    let thumbnailURL = URL(
      string: "\(Constants.cloudfarePrefix)\(result.video_uid().toString())\(Constants.thumbnailSuffix)"
    ) ?? URL(fileURLWithPath: "")

    var profileImageURL: URL?
    if let userPrincipal = try? get_principal(result.created_by_user_principal_id()) {
      profileImageURL = URL(string: propic_from_principal(userPrincipal).toString())
    }

    return FeedResult(
      postID: String(feed.postID),
      videoID: result.video_uid().toString(),
      canisterID: feed.canisterID,
      principalID: result.created_by_user_principal_id().toString(),
      url: videoURL,
      hashtags: result.hashtags().map { $0.as_str().toString() },
      thumbnail: thumbnailURL,
      viewCount: Int64(result.total_view_count()),
      displayName: result.created_by_display_name()?.toString() ?? "",
      postDescription: result.description().toString(),
      profileImageURL: profileImageURL,
      likeCount: Int(result.like_count()),
      isLiked: result.liked_by_me(),
      isNsfw: feed.isNsfw,
      smileyGame: SmileyGame(config: SmileyGameConfig.shared.config, state: .notPlayed)
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

  func logEvent(request: [VideoEventRequest]) async -> Result<Void, FeedError> {
    guard let canisterPrincipalString = authClient.canisterPrincipalString,
          let userPrincipalString = authClient.userPrincipalString else {
      return .failure(FeedError.authError(AuthError.authenticationFailed("No canister principal")))
    }
    guard let baseURL = httpService.baseURL else { return .failure(.networkError(NetworkError.invalidRequest)) }
    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let events = request.map {
        VideoEventDTO(
          event: $0.event,
          canisterId: canisterPrincipalString,
          displayName: $0.displayName,
          hashtagCount: $0.hashtagCount,
          isHotOrNot: $0.isHotOrNot,
          isLoggedIn: $0.isLoggedIn,
          isNsfw: $0.isNsfw,
          likeCount: $0.likeCount,
          postID: $0.postID,
          publisherCanisterId: $0.publisherCanisterID,
          publisherUserId: $0.publisherUserID,
          userID: userPrincipalString,
          videoID: $0.videoID,
          viewCount: Int32($0.viewCount)
        )
      }
      let videoRequestDTO = VideoEventRequestDTO(
        delegatedIdentityWire: swiftWire,
        events: events
      )
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.videoEventPath,
        method: .post,
        headers: ["Content-Type": "application/json"],
        body: try JSONEncoder().encode(videoRequestDTO)
      )
      _ = try await httpService.performRequest(for: endpoint)
      return .success(())
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

  func rechargeWallet() async -> Result<Int64, FeedError> {
    guard let userPrincipalString = authClient.userPrincipalString else {
      return .failure(FeedError.authError(AuthError.authenticationFailed("No user principal")))
    }
    let rechargeRequest = RechargeWalletRequestDTO(
      data: PrincipalDTO(principalID: userPrincipalString)
    )
    guard let baseURL = URL(string: Constants.firebaseBaseURLString) else {
      return .failure(FeedError.networkError(NetworkError.invalidRequest))
    }
    do {
      var httpHeaders = [String: String]()
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(.firebaseError("Failed to fetch user ID token"))
      }
      httpHeaders = [
        "Content-Type": "application/json",
        "Authorization": "Bearer \(userIDToken)"
      ]
      if let appcheckToken = await firebaseService.fetchAppCheckToken() {
        httpHeaders["X-Firebase-AppCheck"] = appcheckToken
      }
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.rechargePath,
        method: .post,
        headers: httpHeaders,
        body: try JSONEncoder().encode(rechargeRequest)
      )
      let data = try await httpService.performRequest(for: endpoint)
      let response = try JSONDecoder().decode(RechargeWalletResponseDTO.self, from: data)
      return .success(Int64(response.coins))
    } catch {
      switch error {
      case let netErr as NetworkError:
        return .failure(FeedError.rechargeError(netErr.localizedDescription))
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

  private func fetchSmileyGamePlayedStatus(for videoID: String) async throws -> Bool {
    guard let userPrincipalID = authClient.userPrincipalString else {
      throw FeedError.unknown("User principal ID not found")
    }

    do {
      let documentPath = "videos/\(videoID)/votes/\(userPrincipalID)"
      let documentExists = try await firebaseService.documentExists(for: documentPath)
      return documentExists
    } catch {
      throw FeedError.firebaseError(error.localizedDescription)
    }
  }
}
// swiftlint: enable type_body_length

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
  var isNsfw: Bool { get }
}
