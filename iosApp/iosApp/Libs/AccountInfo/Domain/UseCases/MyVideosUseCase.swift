//
//  MyVideosUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol MyVideosUseCaseProtocol {
  func execute() async -> Result<AccountInfo, AccountError>
}

class MyVideosUseCase:
  BaseResultUseCase<Void, AccountInfo, AccountError>,
  MyVideosUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<AccountInfo, AccountError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: AccountVideoRequest) async -> Result<AccountInfo, AccountError> {
    await accountRepository.fetchProfile()
  }
}
