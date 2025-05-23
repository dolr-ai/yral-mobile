//
//  SmileyRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class SmileyRepository: SmileyRepositoryProtocol {
  private let firebaseService: FirebaseServiceProtocol

  init(firebaseService: FirebaseServiceProtocol) {
    self.firebaseService = firebaseService
  }

  func fetchSmileyConfig() async -> Result<SmileyConfig, SmileyConfigError> {
    do {
      let response = try await firebaseService.fetchDocument(
        path: Constants.documentPath,
        decodeAs: SmileyConfigDTO.self
      )

      return .success(response.toDomain())
    } catch {
      return .failure(SmileyConfigError.firebaseError(error))
    }
  }
}

extension SmileyRepository {
  enum Constants {
    static let documentPath = "config/smiley_game_v1"
  }
}
