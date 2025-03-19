//
//  MyVideosUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol MyVideosUseCaseProtocol {
  func execute(request: AccountVideoRequest) async -> Result<[FeedResult], AccountError>
}

class MyVideosUseCase:
  BaseResultUseCase<AccountVideoRequest, [FeedResult], AccountError>,
  MyVideosUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: AccountVideoRequest) async -> Result<[FeedResult], AccountError> {
    await accountRepository.fetchVideos(request: request)
  }
}
