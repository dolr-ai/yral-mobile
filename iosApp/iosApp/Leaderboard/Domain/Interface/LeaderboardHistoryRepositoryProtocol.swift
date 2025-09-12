//
//  LeaderboardHistoryRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LeaderboardHistoryRepositoryProtocol {
  func fetchLeaderboardHistory() async -> Result<[LeaderboardResponse], LeaderboardError>
}
