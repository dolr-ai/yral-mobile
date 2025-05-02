//
//  LogoutUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 01/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol LogoutUseCaseProtocol {
  func execute(request: Void) async -> Result<Void, AccountError>
}

class LogoutUseCase:
  BaseResultUseCase<Void, Void, AccountError>,
  LogoutUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: Void) async -> Result<Void, AccountError> {
    await accountRepository.logout()
  }
}
