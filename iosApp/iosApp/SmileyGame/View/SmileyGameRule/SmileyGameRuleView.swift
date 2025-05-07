//
//  SmileyGameRuleView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 22/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SmileyGameRuleView: View {
  @ObservedObject var viewModel: SmileyGameRuleViewModel
  @State private var showRules: Bool = false

  var body: some View {
    ScrollView {
      if let gameRules = viewModel.smileyGameRuleResponse, showRules {
        ForEach(gameRules, id: \.name) { rule in
          GameRuleView(rule: rule)
            .padding(.horizontal, Constants.ruleHorizontalPadding)
            .padding(.bottom, Constants.ruleBottomPadding)
        }
      } else {
        EmptyView()
      }
    }
    .onReceive(viewModel.$state, perform: { state in
      switch state {
      case .success:
        showRules = true
      default:
        break
      }
    })
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    .padding(.top, Constants.pageTopPadding)
    .background(Constants.backgroundColor)
    .task {
      await viewModel.fetchSmileyGameRules()
    }
  }
}

extension SmileyGameRuleView {
  enum Constants {
    static let ruleHorizontalPadding = 16.0
    static let ruleBottomPadding = 20.0
    static let pageTopPadding = 12.0
    static let backgroundColor = YralColor.grey950.swiftUIColor
  }
}
