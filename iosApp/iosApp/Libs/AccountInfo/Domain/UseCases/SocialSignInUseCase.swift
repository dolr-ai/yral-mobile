//
//  SocialSigninUseCaser.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 29/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol SocialSignInUseCaseProtocol {
  func execute(request: SocialProvider) async -> Result<Void, AccountError>
}

class SocialSignInUseCase:
  BaseResultUseCase<SocialProvider, Void, AccountError>,
  SocialSignInUseCaseProtocol {
  let accountRepository: AccountRepositoryProtocol

  init(accountRepository: AccountRepositoryProtocol, crashReporter: CrashReporter) {
    self.accountRepository = accountRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: SocialProvider) async -> Result<Void, AccountError> {
    await accountRepository.socialSignIn(provider: request)
  }
}
