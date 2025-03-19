//
//  ProfileRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

class AccountRepository: AccountRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchAccount() async -> Result<AccountInfo, AccountError> {
    return .success(AccountInfo(imageURL: nil, canisterID: authClient.userPrincipalString ?? ""))
  }

  func fetchVideos(request: AccountVideoRequest) async -> Result<[FeedResult], AccountError> {
    guard let principal = authClient.canisterPrincipal else {
      return .failure(AccountError.authError("No canister principal"))
    }
    do {
      let identity = try self.authClient.generateNewDelegatedIdentity()
      let service = try Service(principal, identity)
      let result = try await service.get_posts_of_this_user_profile_with_pagination_cursor(
        request.startIndex,
        request.offset
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

extension AccountRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
  }
}
