//
//  GameRuleView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 22/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameRuleView: View {
  @ObservedObject var viewModel: GameRuleViewModel
  @State private var showRules: Bool = false

  let onDismiss: () -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: .zero) {
      if let gameRules = viewModel.gameRuleResponse, showRules {
        HStack(alignment: .center, spacing: Constants.hStackSpacing) {
          Image(Constants.backImage)
            .resizable()
            .frame(width: Constants.backImageSize, height: Constants.backImageSize)
            .padding(.leading, Constants.backImageLeadingPadding)
            .onTapGesture {
              onDismiss()
            }

          Text(Constants.screenTitle)
            .font(Constants.screenFont)
            .foregroundColor(Constants.screenTextColor)
        }
        .padding(.bottom, Constants.hStackBottomPadding)

        ScrollView {
          ForEach(gameRules, id: \.name) { rule in
            GameRuleRowView(rule: rule)
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
      await viewModel.fetchGameRules()
    }
  }
}

extension GameRuleView {
  enum Constants {
    static let hStackSpacing = 12.0
    static let hStackBottomPadding = 24.0
    static let backImage = "chevron-left"
    static let backImageSize = 24.0
    static let backImageLeadingPadding = 12.0
    static let screenTitle = "How to play?"
    static let screenFont = YralFont.pt20.bold.swiftUIFont
    static let screenTextColor = YralColor.grey0.swiftUIColor
    static let ruleHorizontalPadding = 16.0
    static let ruleBottomPadding = 20.0
    static let pageTopPadding = 12.0
    static let backgroundColor = YralColor.grey950.swiftUIColor
  }
}
