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

  func castVote(for request: CastVoteQuery) async -> Result<SmileyGameResultResponse, CastVoteError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }

    var httpHeaders = [
      "Content-Type": "application/json"
    ]
    var httpBody = [String: String]()

    do {
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(.unknown("Failed to fetch user ID token"))
      }
      httpHeaders["Authorization"] = "Bearer \(userIDToken)"
    } catch {
      return .failure(.firebaseError(error))
    }

    httpBody["video_id"] = request.videoID
    httpBody["smiley_id"] = request.smileyID
    httpBody["principal_id"] = authClient.userPrincipalString ?? ""

    do {
      let response = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: "cast_vote",
          method: .post,
          headers: httpHeaders,
          body: try? JSONSerialization.data(withJSONObject: httpBody)
        ),
        decodeAs: SmileyGameResultDTO.self
      )
      return .success(SmileyGameResultResponse(
        videoID: request.videoID,
        outcome: response.outcome,
        coins: response.coins,
        coinDelta: response.coinDelta,
        smiley: response.smiley.toDomain()
      ))
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(.network(error))
      default:
        return .failure(.firebaseError(error))
      }
    }
  }
}
