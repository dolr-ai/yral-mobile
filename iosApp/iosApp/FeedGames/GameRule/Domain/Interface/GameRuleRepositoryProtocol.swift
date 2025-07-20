//
//  GameRuleRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol GameRuleRepositoryProtocol {
  func fetchRules(for game: FeedGame) async -> Result<[GameRuleResponse], GameRuleError>
}
