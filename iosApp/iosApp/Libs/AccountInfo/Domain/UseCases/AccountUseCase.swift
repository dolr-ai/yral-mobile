//
//  ProfileUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol AccountUseCaseProtocol {
  func execute() async -> Result<AccountInfo, AccountError>
}

class AccountUseCase:
  BaseResultUseCase<Void, AccountInfo, AccountError>,
  AccountUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<AccountInfo, AccountError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<AccountInfo, AccountError> {
    await accountRepository.fetchProfile()
  }
}
