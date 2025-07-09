//
//  SmileyGameConfig.swift
//  iosApp
//
//  Created by Samarth Paboowal on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class SmileyGameConfig {
  static let shared = SmileyGameConfig()
  private let firebaseService = FirebaseService()

  var config = SmileyConfig(
    smileys: [],
    lossPenalty: 10
  )

  private init() {}

  func fetch() async -> Result<SmileyConfig, SmileyConfigError> {
    guard config.smileys.isEmpty else {
      return .success(config)
    }
    do {
      let response = try await firebaseService.fetchDocument(
        path: Constants.documentPath,
        checkCache: true,
        decodeAs: SmileyConfigDTO.self
      )

      let domainResponse = response.toDomain()
      config = domainResponse
      return .success(response.toDomain())
    } catch {
      return .failure(SmileyConfigError.firebaseError(error))
    }
  }
}

extension SmileyGameConfig {
  enum Constants {
    static let documentPath = "config/smiley_game_v1"
  }
}
