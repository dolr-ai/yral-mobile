//
//  LeaderboardRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import FirebaseFirestore

class LeaderboardRepository: LeaderboardRepositoryProtocol {
  private let firebaseService: FirebaseServiceProtocol
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(
    firebaseService: FirebaseServiceProtocol,
    httpService: HTTPService,
    authClient: AuthClient
  ) {
    self.firebaseService = firebaseService
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchLeaderboard(request: LeaderboardQuery) async -> Result<LeaderboardResponse, LeaderboardError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }

    var httpHeaders = [String: String]()

    do {
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(.unknown(Constants.firebaseUserIDError))
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
      return .failure(.unknown(Constants.principalIDError))
    }

    do {
      let httpBody = request.addingPrincipal(principalID)
      let modifiedHttpBody = [
        "data": httpBody
      ]

      let response = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: Constants.leaderboardPath,
          method: .post,
          headers: httpHeaders,
          body: try? JSONEncoder().encode(modifiedHttpBody)
        ),
        decodeAs: LeaderboardDTO.self
      )

      return .success(response.toDomain())
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
}

extension LeaderboardRepository {
  enum Constants {
    static let leaderboardPath = "leaderboard_v2"
    static let firebaseUserIDError = "Failed to fetch user ID token"
    static let principalIDError = "Failed to fetch princiapl ID"
  }
}
