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
      newVideosSubject.send(newVideos)
      videos.append(contentsOf: newVideos)
      return .success(newVideos)
    case .failure(let error):
      return .failure(error)
    }
  }

  func refreshVideos() async -> Result<[FeedResult], AccountError> {
    let result = await getUserVideos(with: 0, offset: UInt64(Constants.offset))
    switch result {
    case .success(let newChunk):
      let oldChunkCount = min(videos.count, 10)
      if newChunk.count > oldChunkCount {
        videos = newChunk
      } else {
        for item in 0..<newChunk.count {
          guard item < videos.count else { break }
          videos[item] = newChunk[item]
        }
      }
      return .success(videos)

    case .failure(let error):
      return .failure(error)
    }
  }

  private func getUserVideos(
    with startIndex: UInt64,
    offset: UInt64
  ) async -> Result<[FeedResult], AccountError> {
    guard let principalString = authClient.canisterPrincipalString else {
      return .failure(AccountError.authError("No canister principal"))
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
        let feedResult = postResult.map { postDetail in
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
            thumbnail: thumbnailURL,
            postDescription: postDetail.description().toString(),
            likeCount: Int(postDetail.like_count()),
            isLiked: postDetail.liked_by_me())
        }
        return .success(feedResult)
      } else {
        guard let error = result.err_value() else { return .failure(AccountError.unkown("Invalid state")) }
        if error.is_exceeded_max_number_of_items_allowed_in_one_request() {
          return .failure(AccountError.invalidVideoRequest("Exceeded max number of items allowed in one request"))
        } else if error.is_invalid_bounds_passed() {
          return .failure(AccountError.invalidVideoRequest("Invalid bounds passed"))
        } else if error.is_reached_end_of_items_list() {
          return .failure(AccountError.pageEndReached)
        } else {
          return .failure(AccountError.unkown("Invalid state"))
        }
      }
    } catch {
      return .failure(AccountError.rustError(RustError.unknown(error.localizedDescription)))
    }
  }
}

extension ProfileRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let offset = 10
  }
}
