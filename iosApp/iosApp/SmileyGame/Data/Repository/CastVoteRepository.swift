//
//  CastVoteRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class CastVoteRepository: CastVoteRepositoryProtocol {
  private let firebaseService: FirebaseService
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(firebaseService: FirebaseService, httpService: HTTPService, authClient: AuthClient) {
    self.firebaseService = firebaseService
    self.httpService = httpService
    self.authClient = authClient
  }

  // swiftlint: disable function_body_length
  func castVote(for request: CastVoteQuery) async -> Result<SmileyGameResultResponse, CastVoteError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }
    var httpHeaders = [String: String]()
    do {
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(.unknown("Failed to fetch user ID token"))
      }
      httpHeaders = [
        "Content-Type": "application/json",
        "Authorization": "Bearer \(userIDToken)"
      ]
      if let appcheckToken = await firebaseService.fetchAppCheckToken() {
        httpHeaders["X-Firebase-AppCheck"] = appcheckToken
      }
    } catch {
      return .failure(.firebaseError(error))
    }

    guard let principalID = authClient.userPrincipalString else {
      return .failure(.unknown("Failed to fetch princiapl ID"))
    }
    let httpBody = request.addingPrincipal(principalID)

    do {
      let response = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: "cast_vote_v2",
          method: .post,
          headers: httpHeaders,
          body: try? JSONEncoder().encode(httpBody)
        ),
        decodeAs: SmileyGameResultDTO.self
      )
      return .success(SmileyGameResultResponse(
        videoID: request.videoID,
        outcome: response.outcome,
        coins: response.coins,
        coinDelta: response.coinDelta,
        newPosition: response.newPosition,
        smiley: response.smiley.toDomain()
      ))
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(.network(error))
      case let error as CloudFunctionError:
        return .failure(.cloudFunctionError(error))
      default:
        return .failure(.firebaseError(error))
      }
    }
  }
  // swiftlint: enable function_body_length
}
