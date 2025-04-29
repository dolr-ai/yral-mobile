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

  var body: some View {
    ScrollView {
      switch viewModel.state {
      case .success:
        ForEach(viewModel.smileyGameRuleResponse, id: \.name) { rule in
          GameRuleView(rule: rule)
            .padding(.horizontal, Constants.ruleHorizontalPadding)
            .padding(.bottom, Constants.ruleBottomPadding)
        }
      default:
        EmptyView()
      }
    }
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
