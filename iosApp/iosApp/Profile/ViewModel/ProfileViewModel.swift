//
//  ProfileViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Combine

enum ProfilePageState {
  case initialized
  case loading
  case success
  case failure(String)
}

enum ProfilePageEvent {
  case fetchedAccountInfo(AccountInfo)
}

class ProfileViewModel: ObservableObject {
  @Published var state: ProfilePageState = .initialized
  @Published var event: ProfilePageEvent?

  let accountUseCase: AccountUseCase

  init(accountUseCase: AccountUseCase) {
    self.accountUseCase = accountUseCase
  }

  func fetchProfileInfo() async {
    state = .loading
    let result = await accountUseCase.execute()
    await MainActor.run {
      switch result {
      case .success(let profileInfo):
        event = .fetchedAccountInfo(profileInfo)
        state = .success
      case .failure(let error):
        state = .failure(error.localizedDescription)
      }
    }
  }
}
