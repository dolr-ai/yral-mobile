//
//  SmileyGameRuleRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol SmileyGameRuleRepositoryProtocol {
  func fetchSmileyGameRules() async -> Result<[SmileyGameRuleResponse], SmileyGameRuleError>
}
