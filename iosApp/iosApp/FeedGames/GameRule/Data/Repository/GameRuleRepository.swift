//
//  GameRuleRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class GameRuleRepository: GameRuleRepositoryProtocol {
  private let firebaseService: FirebaseServiceProtocol

  init(firebaseService: FirebaseServiceProtocol) {
    self.firebaseService = firebaseService
  }

  func fetchRules(for game: FeedGame) async -> Result<[GameRuleResponse], GameRuleError> {
    do {
      let response = try await firebaseService.fetchCollection(
        from: game == .hon ? Constants.honRulesCollectionPath : Constants.smileyRulesCollectionPath,
        orderBy: ["__name__"],
        descending: false,
        limit: nil,
        decodeAs: GameRuleDTO.self
      )

      return .success(response.map({ $0.toDomain() }))
    } catch {
      return .failure(GameRuleError.firebaseError(error))
    }
  }
}

extension GameRuleRepository {
  enum Constants {
    static let smileyRulesCollectionPath = "smiley_game_rules"
    static let honRulesCollectionPath = "hon_game_rules"
  }
}
