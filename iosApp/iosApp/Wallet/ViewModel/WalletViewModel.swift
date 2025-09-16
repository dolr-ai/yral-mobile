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
}

class WalletViewModel: ObservableObject {
  let accountUseCase: AccountUseCaseProtocol

  @Published var state: WalletPageState = .initalized
  @Published var event: WalletPageEvent?

  init(
    accountUseCase: AccountUseCaseProtocol,
  ) {
    self.accountUseCase = accountUseCase
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
}
