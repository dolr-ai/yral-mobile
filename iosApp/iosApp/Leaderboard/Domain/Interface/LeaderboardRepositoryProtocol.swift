//
//  LeaderboardRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LeaderboardRepositoryProtocol {
  func fetchLeaderboard() async -> Result<LeaderboardResponse, LeaderboardError>
}
