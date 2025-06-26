//
//  SmileyGameRuleDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

final class SmileyGameRuleDIContainer {
  struct Dependencies {
    let firebaseService: FirebaseService
    let crashReporter: CrashReporter
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }

  func makeSmileyGameRuleRepository() -> SmileyGameRuleRepository {
    SmileyGameRuleRepository(firebaseService: dependencies.firebaseService)
  }

  func makeSmileyGameRuleViewModel() -> SmileyGameRuleViewModel {
    let smileyGameRuleRepository = makeSmileyGameRuleRepository()
    return SmileyGameRuleViewModel(
      smileyGameRuleUseCase: SmileyGameRuleUseCase(
        repository: smileyGameRuleRepository,
        crashRepoerter: dependencies.crashReporter
      )
    )
  }

  func makeSmileyGameRuleView(onDismiss: @escaping () -> Void) -> SmileyGameRuleView {
    SmileyGameRuleView(viewModel: makeSmileyGameRuleViewModel(), onDismiss: onDismiss)
  }
}
