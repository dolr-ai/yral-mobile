//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

struct UserInfoView: View {
  @Binding var accountInfo: AccountInfo?
  @Binding var showLoginButton: Bool
  var shouldApplySpacing: Bool
  var delegate: UserInfoViewProtocol?

  init(
    accountInfo: Binding<AccountInfo?>,
    shouldApplySpacing: Bool,
    showLoginButton: Binding<Bool>,
    delegate: UserInfoViewProtocol?
  ) {
    self._accountInfo = accountInfo
    self.shouldApplySpacing = shouldApplySpacing
    self._showLoginButton = showLoginButton
    self.delegate = delegate
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

      if showLoginButton {
        Button {
          delegate?.loginPressed()
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: SignupClickedEventData(pageName: .menu)
          )
        }
        label: {
          Text(Constants.loginButtonTitle)
            .foregroundColor(Constants.loginButtonTextColor)
            .font(Constants.loginButtonFont)
            .frame(maxWidth: .infinity, minHeight: Constants.loginButtonHeight)
            .background(Constants.loginButtonGradient)
            .cornerRadius(Constants.loginButtonCornerRadius)
        }
      } else {
        Button {

        } label: {
          Text(Constants.editButtonTitle)
            .font(Constants.editButtonFont)
            .foregroundColor(Constants.editButtonColor)
            .padding(.vertical, Constants.editButtonVertical)
            .frame(maxWidth: .infinity, alignment: .center)
            .background(
              RoundedRectangle(cornerRadius: Constants.editButtonCornerRadius)
                .fill(Constants.editButtonBackground)
            )
            .overlay(
              RoundedRectangle(cornerRadius: Constants.editButtonCornerRadius)
                .stroke(Constants.editButtonBorder, lineWidth: .one)
            )
        }
        .padding(.vertical, -Constants.editButtonParentVertical)
      }

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
    static let loginButtonTitle = "Login"
    static let loginButtonTextColor =  YralColor.grey50.swiftUIColor
    static let loginButtonFont = YralFont.pt16.bold.swiftUIFont
    static let loginButtonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let loginButtonHeight = 45.0
    static let loginButtonCornerRadius: CGFloat = 8
    static let dividerColor = YralColor.grey800.swiftUIColor

    static let editButtonTitle = "Edit Profile"
    static let editButtonFont = YralFont.pt14.semiBold.swiftUIFont
    static let editButtonColor = YralColor.grey50.swiftUIColor
    static let editButtonBackground = YralColor.grey800.swiftUIColor
    static let editButtonBorder = YralColor.grey700.swiftUIColor
    static let editButtonCornerRadius = 8.0
    static let editButtonVertical = 10.0
    static let editButtonParentVertical = 14.0
  }
}

protocol UserInfoViewProtocol: Any {
  func loginPressed()
}
