//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UserInfoView: View {
  @Binding var accountInfo: AccountInfo?
  var shouldApplySpacing: Bool

  init(accountInfo: Binding<AccountInfo?>, shouldApplySpacing: Bool) {
    self._accountInfo = accountInfo
    self.shouldApplySpacing = shouldApplySpacing
  }

  var body: some View {
    VStack(spacing: Constants.verticalSpacing) {
      if shouldApplySpacing {
        Spacer().frame(width: Constants.verticalSpacing)
      }
      HStack(spacing: Constants.horizontalSpacing) {
        URLImage(url: accountInfo?.imageURL)
          .frame(
            width: Constants.imageSize,
            height: Constants.imageSize
          )
          .clipShape(Circle())
          .overlay(Circle().stroke(Color.white, lineWidth: .one))
        Text(accountInfo?.canisterID ?? "")
          .font(Constants.profileLabelFont)
          .foregroundColor(Constants.profileLabelColor)
          .lineLimit(.two)
      }
      .frame(maxWidth: .infinity, alignment: .leading)
//      VStack(alignment: .leading, spacing: Constants.secondaryVStackSpacing) {
//        Button { }
//        label: {
//          Text(Constants.loginButtonTitle)
//            .foregroundColor(Constants.loginButtonTextColor)
//            .font(Constants.loginButtonFont)
//            .frame(maxWidth: .infinity, minHeight: Constants.loginButtonHeight)
//            .background(Constants.loginButtonGradient)
//            .cornerRadius(Constants.loginButtonCornerRadius)
//        }
//        Text(Constants.loginMessageString)
//          .foregroundColor(Constants.loginLabelColor)
//          .font(Constants.loginLabelFont)
//      }
      Rectangle()
        .fill(Constants.dividerColor)
        .frame(height: .one)
    }
    .padding([.horizontal], shouldApplySpacing ? Constants.horizontalPadding : .zero)
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

extension UserInfoView {
  enum Constants {
    static let verticalSpacing: CGFloat = 30
    static let horizontalSpacing: CGFloat = 16
    static let horizontalPadding: CGFloat = 16
    static let imageSize: CGFloat = 60
    static let profileLabelFont = YralFont.pt14.medium.swiftUIFont
    static let profileLabelColor =  YralColor.grey500.swiftUIColor
    static let secondaryVStackSpacing = 16.0
    static let loginButtonTitle = "Login"
    static let loginButtonTextColor =  YralColor.grey50.swiftUIColor
    static let loginButtonFont = YralFont.pt16.bold.swiftUIFont
    static let loginMessageString = "Your Yral account has been setup. Login with Google to not lose progress."
    static let loginLabelColor = YralColor.grey0.swiftUIColor
    static let loginLabelFont = YralFont.pt12.medium.swiftUIFont
    static let loginButtonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.37, green: 0.03, blue: 0.22), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let loginButtonHeight = 45.0
    static let loginButtonCornerRadius: CGFloat = 8
    static let dividerColor = YralColor.grey800.swiftUIColor
  }
}
