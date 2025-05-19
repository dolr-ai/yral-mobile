//
//  CastVoteRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol CastVoteRepositoryProtocol {
  func castVote(for request: CastVoteQuery) async -> Result<SmileyGameResultResponse, CastVoteError>
}
