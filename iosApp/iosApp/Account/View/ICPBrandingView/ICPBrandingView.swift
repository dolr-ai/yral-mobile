//
//  ICPBrandingView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct ICPBrandingView: View {
  var body: some View {
    HStack(spacing: Constants.hStackSpacing) {
      Image(Constants.icpImage)
        .resizable()
        .frame(width: Constants.logoWidth, height: Constants.logoHeight)
        .padding(.vertical, Constants.elementPadding)
      VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
        Text(Constants.poweredByText)
          .font(Constants.font)
          .foregroundColor(Constants.textColor)
        Text(Constants.icpText)
          .font(Constants.font.weight(.bold))
          .foregroundColor(Constants.textColor)
      }
      .padding(.vertical, Constants.elementPadding)
    }
    .padding(.horizontal, Constants.padding)
    .background(Constants.background)
    .cornerRadius(Constants.cornerRadius)
  }
}

extension ICPBrandingView {
  enum Constants {
    static let icpImage = "ic_logo"
    static let poweredByText = "Powered by"
    static let icpText = "INTERNET COMPUTER"
    static let hStackSpacing: CGFloat = 5
    static let vStackSpacing: CGFloat = 4.0
    static let logoWidth: CGFloat = 85
    static let logoHeight: CGFloat = 48
    static let padding: CGFloat = 16
    static let elementPadding = 8.0
    static let cornerRadius: CGFloat = 8
    static let font = Font.custom("Kumbh Sans", size: 14)
      .weight(.semibold)
    static let textColor = Color.white
    static let background = Color(red: 0.13, green: 0.13, blue: 0.13)
  }
}
