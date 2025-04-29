//
//  ProfileViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum AccountPageState: Equatable {
  case initalized
  case loading
  case successfullyFetched(AccountInfo)
  case failure(Error)

  static func == (lhs: AccountPageState, rhs: AccountPageState) -> Bool {
    switch (lhs, rhs) {
    case (.initalized, .initalized):
      return true
    case (.loading, .loading):
      return true
    case (.successfullyFetched(let lhsInfo), .successfullyFetched(let rhsInfo)):
      return lhsInfo == rhsInfo
    case (.failure(let lhsError), .failure(let rhsError)):
      return lhsError.localizedDescription == rhsError.localizedDescription
    default:
      return false
    }
  }
}

enum AccountPageEvent {
  case socialSignInSuccess
  case socialSignInFailure
}

class AccountViewModel: ObservableObject {
  let accountUseCase: AccountUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol

  @Published var state: AccountPageState = .initalized
  @Published var event: AccountPageEvent?

  init(accountUseCase: AccountUseCaseProtocol, socialSignInUseCase: SocialSignInUseCaseProtocol) {
    self.accountUseCase = accountUseCase
    self.socialSignInUseCase = socialSignInUseCase
  }

  @MainActor func fetchProfileInfo() async {
    state = .loading
    let result = await accountUseCase.execute()
    switch result {
    case .success(let profileInfo):
      state = .successfullyFetched(profileInfo)
    case .failure(let error):
      state = .failure(error)
    }
  }

  func socialSignIn(request: SocialProvider) async {
    let result = await self.socialSignInUseCase.execute(request: request)
    await MainActor.run {
      switch result {
      case .success:
        self.event = .socialSignInSuccess
      case .failure:
        self.event = .socialSignInFailure
      }
    }
  }
}
