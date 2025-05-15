//
//  SmileyRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol SmileyRepositoryProtocol {
  func fetchSmileyConfig() async -> Result<SmileyConfig, SmileyConfigError>
}
