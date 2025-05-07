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

  let onDismiss: () -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      if let gameRules = viewModel.smileyGameRuleResponse, showRules {
        HStack(alignment: .center, spacing: 12) {
          Image("chevron-left")
            .resizable()
            .frame(width: 24, height: 24)
            .padding(.leading, 12)
            .onTapGesture {
              onDismiss()
            }

          Text("How to play?")
            .font(YralFont.pt20.bold.swiftUIFont)
            .foregroundColor(YralColor.grey0.swiftUIColor)
        }
        .padding(.bottom, 24)

        ScrollView {
          ForEach(gameRules, id: \.name) { rule in
            GameRuleView(rule: rule)
              .padding(.horizontal, Constants.ruleHorizontalPadding)
              .padding(.bottom, Constants.ruleBottomPadding)
          }
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
