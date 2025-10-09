//
//  WalletViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum WalletPageState: Equatable {
  case initalized
  case loading
  case success
  case failure(Error)

  static func == (lhs: WalletPageState, rhs: WalletPageState) -> Bool {
    switch (lhs, rhs) {
    case (.initalized, .initalized):
      return true
    case (.loading, .loading):
      return true
    case (.success, .success):
      return true
    case (.failure(let lhsError), .failure(let rhsError)):
      return lhsError.localizedDescription == rhsError.localizedDescription
    default:
      return false
    }
  }
}

enum WalletPageEvent: Equatable {
  case accountInfoFetched(AccountInfo)
  case btcBalanceFetched(Double)
  case exchangeRateFetched(Double)
  case videoViewedRewardsStatusFetched(Bool)
}

class WalletViewModel: ObservableObject {
  let accountUseCase: AccountUseCaseProtocol
  let btcBalanceUseCase: FetchBTCBalanceUseCaseProtocol
  let exchangeRateUseCase: ExchangeRateUseCaseProtocol
  let videoViewedRewardsUseCase: VideoViewedRewardsUseCaseProtocol

  @Published var state: WalletPageState = .initalized
  @Published var event: WalletPageEvent?

  init(
    accountUseCase: AccountUseCaseProtocol,
    btcBalanceUseCase: FetchBTCBalanceUseCaseProtocol,
    exchangeRateUseCase: ExchangeRateUseCaseProtocol,
    videoViewedRewardsUseCase: VideoViewedRewardsUseCaseProtocol
  ) {
    self.accountUseCase = accountUseCase
    self.btcBalanceUseCase = btcBalanceUseCase
    self.exchangeRateUseCase = exchangeRateUseCase
    self.videoViewedRewardsUseCase = videoViewedRewardsUseCase
  }

  @MainActor func fetchAccountInfo() async {
    state = .loading
    let result = await accountUseCase.execute()
    switch result {
    case .success(let accountInfo):
      state = .success
      event = .accountInfoFetched(accountInfo)
    case .failure(let error):
      state = .failure(error)
    }
  }

  @MainActor func fetchBTCBalance() async {
    let result = await btcBalanceUseCase.execute()
    switch result {
    case .success(let btcBalance):
      event = .btcBalanceFetched(btcBalance)
    case .failure(let error):
      state = .failure(error)
    }
  }

  @MainActor func fetchExchangeRate() async {
    let result = await exchangeRateUseCase.execute()
    switch result {
    case .success(let exchangeRate):
      event = .exchangeRateFetched(exchangeRate)
    case .failure(let error):
      state = .failure(error)
    }
  }

  @MainActor func fetchVideoViewedRewardsStatus() async {
    let result = await videoViewedRewardsUseCase.execute()
    switch result {
    case .success(let status):
      event = .videoViewedRewardsStatusFetched(status)
    case .failure:
      break
    }
  }
}
