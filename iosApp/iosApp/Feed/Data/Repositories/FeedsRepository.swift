//
//  FeedsRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

class FeedsRepository: FeedRepositoryProtocol {
  private let httpService: HTTPService
  private let mlClient: MlFeed_MLFeedNIOClient
  private let authClient: AuthClient

  init(httpService: HTTPService, mlClient: MlFeed_MLFeedNIOClient, authClient: AuthClient) {
    self.httpService = httpService
    self.mlClient = mlClient
    self.authClient = authClient
  }

  func fetchFeed(request: FeedRequest) async -> Result<[FeedResult], Error> {
    var mlRequest = MlFeed_FeedRequest()
    mlRequest.canisterID = request.canisterId
    mlRequest.filterPosts = request.filteredPosts
    mlRequest.numResults = UInt32(request.numResults)
    do {
      let response = try mlClient.get_feed_clean(
        mlRequest
      ).response.wait()
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
      print(error)
      return .failure(error)
    }
  }
}

extension FeedsRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
  }
}
