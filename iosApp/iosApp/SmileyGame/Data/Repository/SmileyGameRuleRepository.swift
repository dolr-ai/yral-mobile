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
  private let firebaseService: FirebaseService

  init(firebaseService: FirebaseService) {
    self.firebaseService = firebaseService
  }

  func fetchSmileyGameRules() async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError> {
    do {
      let response = try await firebaseService.fetchCollection(
        from: Constants.collectionPath,
        orderBy: FieldPath.documentID(),
        decodeAs: SmileyGameRuleDTO.self
      )

      return .success(response.map({ $0.toDomain() }))
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(SmileyGameRuleError.networkError(error))
      default:
        return .failure(SmileyGameRuleError.unknown(error.localizedDescription))
      }
    }
  }
}

extension SmileyGameRuleRepository {
  enum Constants {
    static let collectionPath = "smiley_game_rules"
  }
}
