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
  case logoutSuccess
  case logoutFailure
  case deleteSuccess
  case deleteFailure
}

class AccountViewModel: ObservableObject {
  let accountUseCase: AccountUseCaseProtocol
  let socialSignInUseCase: SocialSignInUseCaseProtocol
  let logoutUseCase: LogoutUseCaseProtocol
  let deleteUseCase: DeleteUseCaseProtocol

  @Published var state: AccountPageState = .initalized
  @Published var event: AccountPageEvent?

  init(
    accountUseCase: AccountUseCaseProtocol,
    socialSignInUseCase: SocialSignInUseCaseProtocol,
    logoutUseCase: LogoutUseCaseProtocol,
    deleteUseCase: DeleteUseCaseProtocol
  ) {
    self.accountUseCase = accountUseCase
    self.socialSignInUseCase = socialSignInUseCase
    self.logoutUseCase = logoutUseCase
    self.deleteUseCase = deleteUseCase
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

  func logout() async {
    let result = await logoutUseCase.execute(request: ())
    await MainActor.run {
      switch result {
      case .success(let success):
        self.event = .logoutSuccess
      case .failure(let failure):
        self.event = .logoutFailure
      }
    }
  }

  func delete() async {
    let result = await deleteUseCase.execute(request: ())
    await MainActor.run {
      switch result {
      case .success(let success):
        self.event = .deleteSuccess
      case .failure(let failure):
        self.event = .deleteFailure
      }
    }
  }
}
