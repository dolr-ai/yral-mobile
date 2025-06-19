//
//  SmileyGameRuleRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import FirebaseFirestore

class SmileyGameRuleRepository: SmileyGameRuleRepositoryProtocol {
  private let firebaseService: FirebaseServiceProtocol

  init(firebaseService: FirebaseServiceProtocol) {
    self.firebaseService = firebaseService
  }

  func fetchSmileyGameRules() async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError> {
    do {
      let response = try await firebaseService.fetchCollection(
        from: Constants.collectionPath,
        orderBy: ["__name__"],
        descending: false,
        limit: nil,
        decodeAs: SmileyGameRuleDTO.self
      )

      return .success(response.map({ $0.toDomain() }))
    } catch {
      return .failure(SmileyGameRuleError.firebaseError(error))
    }
  }
}

extension SmileyGameRuleRepository {
  enum Constants {
    static let collectionPath = "smiley_game_rules"
  }
}
