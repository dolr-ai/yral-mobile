//
//  AccountRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 21/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import Combine

class ProfileRepository: ProfileRepositoryProtocol {
  @Published private(set) var videos: [FeedResult] = []

  private let newVideosSubject = PassthroughSubject<[FeedResult], Never>()
  var newVideosPublisher: AnyPublisher<[FeedResult], Never> {
    newVideosSubject.eraseToAnyPublisher()
  }

  private let deletedVideoSubject = PassthroughSubject<[FeedResult], Never>()
  var deletedVideoPublisher: AnyPublisher<[FeedResult], Never> {
    deletedVideoSubject.eraseToAnyPublisher()
  }

  var videosPublisher: AnyPublisher<[FeedResult], Never> {
      $videos.eraseToAnyPublisher()
  }

  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchVideos(request: ProfileVideoRequest) async -> Result<[FeedResult], AccountError> {
    let result = await getUserVideos(with: request.startIndex, offset: request.offset)
    switch result {
    case .success(let newVideos):
      let filteredNewVideos = newVideos.filter { newVideo in
        !videos.contains { $0.postID == newVideo.postID }
      }
      newVideosSubject.send(filteredNewVideos)
      videos.append(contentsOf: filteredNewVideos)
      return .success(newVideos)
    case .failure(let error):
      return .failure(error)
    }
  }

  func refreshVideos(shouldPurge: Bool) async -> Result<[FeedResult], AccountError> {
    if shouldPurge {
      self.deletedVideoSubject.send(videos)
      self.videos.removeAll()
    }
    let result = await getUserVideos(with: .zero, offset: UInt64(Constants.offset))
    switch result {
    case .success(let newChunk):
      let newVideoIDs = Set(newChunk.map { $0.postID })
      let filteredOldVideos = videos.filter { !newVideoIDs.contains($0.postID) }
      await MainActor.run { [weak self] in
        guard let self = self else { return }
        videos = newChunk + filteredOldVideos
      }
      return .success(videos)

    case .failure(let error):
      return .failure(error)
    }
  }

  func deleteVideo(request: DeleteVideoRequest) async -> Result<Void, AccountError> {
    guard let principalString = authClient.canisterPrincipalString else {
      return .failure(AccountError.authError(AuthError.invalidRequest("No canister principal")))
    }
    guard let baseURL = httpService.baseURL else { return .failure(.invalidInfo("No base URL found")) }
    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let deleteVideoDTO = DeleteVideoRequestDTO(
        canisterId: principalString,
        postId: request.postId,
        videoId: request.videoId,
        delegatedIdentityWire: swiftWire
      )
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.deleteVideoPath,
        method: .delete,
        headers: ["Content-Type": "application/json"],
        body: try JSONEncoder().encode(deleteVideoDTO)
      )
      _ = try await httpService.performRequest(for: endpoint)
      if let index = videos.firstIndex(where: { $0.postID == String(request.postId) }) {
        let deletedVideo = videos.remove(at: index)
        deletedVideoSubject.send([deletedVideo])
      }
      return .success(())
    } catch {
      switch error {
      case let netErr as NetworkError:
        return .failure(AccountError.networkError(netErr.localizedDescription))
      case let authErr as AuthError:
        return .failure(AccountError.authError(authErr))
      default:
        return .failure(.unknown(error.localizedDescription))
      }
    }
  }

  // swiftlint: disable function_body_length
  private func getUserVideos(
    with startIndex: UInt64,
    offset: UInt64
  ) async -> Result<[FeedResult], AccountError> {
    guard let principalString = authClient.canisterPrincipalString else {
      return .failure(AccountError.authError(AuthError.invalidRequest("No canister principal")))
    }
    do {
      let identity = try self.authClient.generateNewDelegatedIdentity()
      let principal = try get_principal(principalString)
      let service = try Service(principal, identity)
      let result = try await service.get_posts_of_this_user_profile_with_pagination_cursor(
        startIndex,
        offset
      )
      if result.is_ok() {
        guard let postResult = result.ok_value() else { return .success([FeedResult]()) }
        let result = postResult.filter {
          !$0.status().is_banned_due_to_user_reporting() &&
          !$0.is_nsfw()
        }

        let feedResult = result.map { postDetail in
          let videoURL = URL(
            string: "\(Constants.cloudfarePrefix)\(postDetail.video_uid().toString())\(Constants.cloudflareSuffix)"
          ) ?? URL(fileURLWithPath: "")
          let thumbnailURL = URL(
            string: "\(Constants.cloudfarePrefix)\(postDetail.video_uid().toString())\(Constants.thumbnailSuffix)"
          ) ?? URL(fileURLWithPath: "")

          return FeedResult(
            postID: String(postDetail.id()),
            videoID: postDetail.video_uid().toString(),
            canisterID: authClient.canisterPrincipalString ?? "",
            principalID: authClient.userPrincipalString ?? "",
            url: videoURL,
            hashtags: postDetail.hashtags().map { $0.as_str().toString() },
            thumbnail: thumbnailURL,
            viewCount: Int64(postDetail.total_view_count()),
            displayName: postDetail.created_by_display_name()?.toString() ?? "",
            postDescription: postDetail.description().toString(),
            likeCount: Int(postDetail.like_count()),
            isLiked: postDetail.liked_by_me(),
            isNsfw: false
          )
        }
        return .success(feedResult)
      } else {
        guard let error = result.err_value() else { return .failure(AccountError.unknown("Invalid state")) }
        if error.is_exceeded_max_number_of_items_allowed_in_one_request() {
          return .failure(AccountError.invalidVideoRequest("Exceeded max number of items allowed in one request"))
        } else if error.is_invalid_bounds_passed() {
          return .failure(AccountError.invalidVideoRequest("Invalid bounds passed"))
        } else if error.is_reached_end_of_items_list() {
          return .failure(AccountError.pageEndReached)
        } else {
          return .failure(AccountError.unknown("Invalid state"))
        }
      }
    } catch {
      return .failure(AccountError.rustError(RustError.unknown(error.localizedDescription)))
    }
  }
  // swiftlint: enable function_body_length

  private func swiftDelegatedIdentityWire(from rustWire: DelegatedIdentityWire) throws -> SwiftDelegatedIdentityWire {
    let wireJsonString = delegated_identity_wire_to_json(rustWire).toString()
    guard let data = wireJsonString.data(using: .utf8) else {
      throw AuthError.authenticationFailed("Failed to convert wire JSON string to Data")
    }
    return try JSONDecoder().decode(SwiftDelegatedIdentityWire.self, from: data)
  }
}

extension ProfileRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let deleteVideoPath = "/api/v1/posts"
    static let offset = 10
  }
}
