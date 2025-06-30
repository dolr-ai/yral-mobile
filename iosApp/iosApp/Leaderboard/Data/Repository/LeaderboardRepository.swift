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
      checkCache: false,
      decodeAs: LeaderboardRowDTO.self
    )

    do {
      let (topSnap, userSnap) = try await (topTenTask, userTask)

      var startingPosition = 1
      var modifiedTopTenDTO: [LeaderboardRowDTO] = []

      for index in 0 ..< topSnap.count {
        if index > 0 && topSnap[index].coins < topSnap[index - 1].coins {
          startingPosition += 1
        }

        modifiedTopTenDTO.append(
          LeaderboardRowDTO(
            id: topSnap[index].id,
            position: startingPosition,
            coins: topSnap[index].coins
          )
        )
      }

      let leaderboardDTO = LeaderboardDTO(
        userRow: userSnap,
        rows: modifiedTopTenDTO
      )

      return .success(leaderboardDTO.toDomain())
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
