//
//  RechargeUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Combine

protocol RechargeUseCaseProtocol {
  func execute(request: Void) async -> Result<Int64, FeedError>
}

class RechargeUseCase:
  BaseResultUseCase<Void, Int64, FeedError>,
  RechargeUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: Void) async -> Result<Int64, FeedError> {
    await feedRepository.rechargeWallet()
  }
}
