//
//  DeleteUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 09/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol DeleteUseCaseProtocol {
  func execute(request: Void) async -> Result<Void, AccountError>
}

class DeleteUseCase:
  BaseResultUseCase<Void, Void, AccountError>,
  DeleteUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: Void) async -> Result<Void, AccountError> {
    await accountRepository.delete()
  }
}
