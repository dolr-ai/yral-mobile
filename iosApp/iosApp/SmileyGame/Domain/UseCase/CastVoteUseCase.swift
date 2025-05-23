//
//  CastVoteUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol CastVoteUseCaseProtocol {
  func execute(request: CastVoteQuery) async -> Result<SmileyGameResultResponse, CastVoteError>
}

class CastVoteUseCase: BaseResultUseCase<CastVoteQuery, SmileyGameResultResponse, CastVoteError>,
                       CastVoteUseCaseProtocol {
  private let castVoteRepository: CastVoteRepositoryProtocol

  init(castVoteRepository: CastVoteRepositoryProtocol, crashReporter: CrashReporter) {
    self.castVoteRepository = castVoteRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: CastVoteQuery) async -> Result<SmileyGameResultResponse, CastVoteError> {
    await castVoteRepository.castVote(for: request)
  }
}
