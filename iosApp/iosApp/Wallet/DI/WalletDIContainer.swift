//
//  WalletDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

final class WalletDIContainer {
  struct Dependencies {
    let httpService: HTTPService
    let authClient: AuthClient
    let firebaseService: FirebaseService
    let crashReporter: CrashReporter
    let session: SessionManager
    let accountUseCase: AccountUseCaseProtocol
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  @MainActor func makeWalletView() -> WalletView {
    let repository = WalletRepository(
      httpService: dependencies.httpService,
      authClient: dependencies.authClient,
      firebaseService: dependencies.firebaseService
    )
    return WalletView(
      viewModel: WalletViewModel(
        accountUseCase: dependencies.accountUseCase,
        btcBalanceUseCase: FetchBTCBalanceUseCase(
          walletRepository: repository,
          crashReporter: dependencies.crashReporter
        ),
        exchangeRateUseCase: ExchangeRateUseCase(
          walletRepository: repository,
          crashReporter: dependencies.crashReporter
        )
      )
    )
  }
}
