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
  private let authClient: AuthClient

  init(firebaseService: FirebaseServiceProtocol, authClient: AuthClient) {
    self.firebaseService = firebaseService
    self.authClient = authClient
  }

  func fetchLeaderboard() async -> Result<LeaderboardResponse, LeaderboardError> {
    guard let userPrincipalID = authClient.userPrincipalString else {
      return .failure(.unknown(Constants.principalError))
    }

    async let topTenTask = firebaseService.fetchCollection(
      from: Constants.usersCollectionPath,
      orderBy: [Constants.coinsField],
      descending: true,
      limit: Constants.leaderboardLimit,
      decodeAs: LeaderboardRowDTO.self
    )

    async let userTask = firebaseService.fetchDocument(
      path: "\(Constants.usersCollectionPath)/\(userPrincipalID)",
      decodeAs: LeaderboardRowDTO.self
    )

    do {
      let (topSnap, userSnap) = try await (topTenTask, userTask)

      let leaderboardDTO = LeaderboardDTO(
        userRow: userSnap,
        rows: topSnap
      )

      let userPositionQuery = Firestore.firestore()
        .collection(Constants.usersCollectionPath)
        .whereField(Constants.coinsField, isGreaterThan: userSnap.coins)
      let aggregateQuery = try await userPositionQuery.count.getAggregation(source: .server)
      let userPosition = Int(truncating: aggregateQuery.count) + .one

      return .success(leaderboardDTO.toDomain(userPosition: userPosition))
    } catch {
      return .failure(LeaderboardError.firebaseError(error))
    }
  }
}

extension LeaderboardRepository {
  enum Constants {
    static let usersCollectionPath = "users"
    static let coinsField = "coins"
    static let leaderboardLimit: Int = 10
    static let principalError = "Could not find user principal ID"
  }
}
