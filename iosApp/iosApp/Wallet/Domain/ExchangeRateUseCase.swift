//
//  Untitled.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ExchangeRateUseCaseProtocol {
  func execute() async -> Result<Double, WalletError>
}

class ExchangeRateUseCase:
  BaseResultUseCase<Void, Double, WalletError>,
  ExchangeRateUseCaseProtocol {
  let walletRepository: WalletRepositoryProtocol

  init(walletRepository: WalletRepositoryProtocol, crashReporter: CrashReporter) {
    self.walletRepository = walletRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<Double, WalletError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<Double, WalletError> {
    await walletRepository.fetchBTCExchangeRate()
  }
}
