//
//  SmileyGameRuleViewModel.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum SmileyGameRuleViewState {
  case initialized
  case loading
  case success
  case failure(Error)

  static func == (lhs: SmileyGameRuleViewState, rhs: SmileyGameRuleViewState) -> Bool {
    switch (lhs, rhs) {
    case (.initialized, .initialized),
      (.loading, .loading),
      (.success, .success):
      return true
    case (.failure, .failure):
      return true
    default:
      return false
    }
  }
}

class SmileyGameRuleViewModel: ObservableObject {
  let smileyGameRuleUseCase: SmileyGameRuleUseCaseProtocol
  var smileyGameRuleResponse: [SmileyGameRuleResponse]!

  @Published var state: SmileyGameRuleViewState = .initialized

  init(smileyGameRuleUseCase: SmileyGameRuleUseCaseProtocol) {
    self.smileyGameRuleUseCase = smileyGameRuleUseCase
  }

  func fetchSmileyGameRules() async {
    let result = await smileyGameRuleUseCase.execute()
    await MainActor.run {
      switch result {
      case .success(let success):
        self.smileyGameRuleResponse = success
        self.state = .success
      case .failure(let failure):
        self.state = .failure(failure)
        print("Failed to fetch smiley game rules: \(failure)")
      }
    }
  }
}
