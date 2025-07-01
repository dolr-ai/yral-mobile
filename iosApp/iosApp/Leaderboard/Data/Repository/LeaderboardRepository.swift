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

  // swiftlint: disable function_body_length
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

      var startingPosition: Int = .one
      var modifiedTopTenDTO: [LeaderboardRowDTO] = []
      let modifiedUserDTO: LeaderboardRowDTO

      for index in .zero ..< topSnap.count {
        if index > .zero && topSnap[index].coins < topSnap[index - .one].coins {
          startingPosition += .one
        }

        modifiedTopTenDTO.append(
          LeaderboardRowDTO(
            id: topSnap[index].id,
            position: startingPosition,
            coins: topSnap[index].coins
          )
        )
      }

      if let userIndex = modifiedTopTenDTO.firstIndex(where: { $0.coins == userSnap.coins }) {
        modifiedUserDTO = LeaderboardRowDTO(
          id: userSnap.id,
          position: modifiedTopTenDTO[userIndex].position,
          coins: userSnap.coins
        )
      } else {
        let userPositionQuery = Firestore.firestore()
          .collection(Constants.usersCollectionPath)
          .whereField(Constants.coinsField, isGreaterThan: userSnap.coins)
        let aggregateQuery = try await userPositionQuery.count.getAggregation(source: .server)
        let userPosition = Int(truncating: aggregateQuery.count) + .one

        modifiedUserDTO = LeaderboardRowDTO(
          id: userSnap.id,
          position: userPosition,
          coins: userSnap.coins
        )
      }

      let leaderboardDTO = LeaderboardDTO(
        userRow: modifiedUserDTO,
        rows: modifiedTopTenDTO
      )

      return .success(leaderboardDTO.toDomain())
    } catch {
      return .failure(LeaderboardError.firebaseError(error))
    }
  }
  // swiftlint: enable function_body_length
}

extension LeaderboardRepository {
  enum Constants {
    static let usersCollectionPath = "users"
    static let coinsField = "coins"
    static let leaderboardLimit: Int = 10
    static let principalError = "Could not find user principal ID"
  }
}
