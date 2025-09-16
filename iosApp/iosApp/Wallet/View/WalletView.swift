//
//  WalletView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct WalletView: View {
  @State var accountInfo: AccountInfo?

  var body: some View {
    VStack(spacing: .zero) {
      VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
        Text(Constants.navigationTitle)
          .font(Constants.navigationTitleFont)
          .foregroundColor(Constants.navigationTitleTextColor)
          .padding(Constants.navigationTitlePadding)
        UserInfoView(
          accountInfo: $accountInfo,
          shouldApplySpacing: false,
          showLoginButton: Binding(get: { false }, set: { _ in }),
          delegate: nil
        )
      }
      .padding(.horizontal, Constants.horizontalPadding)
      Spacer()
    }
  }
}

extension WalletView {
  enum Constants {
    static let vStackSpacing: CGFloat = 24.0
    static let horizontalPadding: CGFloat = 16.0

    static let navigationTitle: String = "My Wallet"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let navigationTitlePadding = EdgeInsets(
      top: 20.0,
      leading: 0.0,
      bottom: 16.0,
      trailing: 0.0
    )
  }
}
