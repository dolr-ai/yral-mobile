//
//  LikeRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

class LikeRepository: LikeRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func toggleLikeStatus(for request: LikeQuery) async -> Result<LikeResult, FeedError> {
    let identity: DelegatedIdentity
    do {
      identity = try self.authClient.generateNewDelegatedIdentity()
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
    do {
      let principal = try get_principal(request.canisterID)
      let service = try Service(principal, identity)
      let status = try await service.update_post_toggle_like_status_by_caller(UInt64(request.postID))
      return .success(LikeResult(status: status, index: request.index))
    } catch {
      return .failure(FeedError.rustError(RustError.unknown(error.localizedDescription)))
    }
  }
}
