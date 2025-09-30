//
//  FetchBTCBalanceUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol FetchBTCBalanceUseCaseProtocol {
  func execute() async -> Result<Double, WalletError>
}

class FetchBTCBalanceUseCase:
  BaseResultUseCase<Void, Double, WalletError>,
  FetchBTCBalanceUseCaseProtocol {
  let walletRepository: WalletRepositoryProtocol

  init(walletRepository: WalletRepositoryProtocol, crashReporter: CrashReporter) {
    self.walletRepository = walletRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<Double, WalletError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<Double, WalletError> {
    let satsResult: Result<UInt32, WalletError> = await walletRepository.fetchSatoshiBalance()
    return satsResult.map { sats in
      let satsDecimal = Decimal(sats)
      let divisor = Decimal(100_000_000)
      let btcDecimal = satsDecimal / divisor
      return NSDecimalNumber(decimal: btcDecimal).doubleValue
    }
  }
}
