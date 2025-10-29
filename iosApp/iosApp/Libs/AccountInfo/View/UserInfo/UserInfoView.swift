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
  @Binding var showEditProfileButton: Bool
  var delegate: UserInfoViewProtocol?

  init(
    accountInfo: Binding<AccountInfo?>,
    showLoginButton: Binding<Bool>,
    showEditProfileButton: Binding<Bool>,
    delegate: UserInfoViewProtocol?
  ) {
    self._accountInfo = accountInfo
    self._showLoginButton = showLoginButton
    self._showEditProfileButton = showEditProfileButton
    self.delegate = delegate
  }

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.verticalSpacing) {
      HStack(spacing: .zero) {
        URLImage(url: accountInfo?.imageURL)
          .frame(
            width: Constants.imageSize,
            height: Constants.imageSize
          )
          .clipShape(Circle())
          .overlay(Circle().stroke(Color.white, lineWidth: .one))
          .padding(.trailing, 16)

        VStack(alignment: .leading, spacing: 10) {
          Text(accountInfo?.username ?? "")
            .font(Constants.usernameFont)
            .foregroundColor(Constants.usernameColor)
            .lineLimit(.one)

          if let followers = accountInfo?.followers,
             let following = accountInfo?.following,
             let gamesPlayed = accountInfo?.gamesPlayed {
            HStack(spacing: .zero) {
              VStack(alignment: .leading, spacing: 4) {
                Text("0")
                  .font(Constants.detailInfoFont)
                  .foregroundColor(Constants.detailInfoColor)

                Text("Followers")
                  .font(Constants.detailFont)
                  .foregroundColor(Constants.detailColor)
              }

              Spacer(minLength: .zero)

              VStack(alignment: .leading, spacing: 4) {
                Text("0")
                  .font(Constants.detailInfoFont)
                  .foregroundColor(Constants.detailInfoColor)

                Text("Following")
                  .font(Constants.detailFont)
                  .foregroundColor(Constants.detailColor)
              }

              Spacer(minLength: .zero)

              VStack(alignment: .leading, spacing: 4) {
                Text("0")
                  .font(Constants.detailInfoFont)
                  .foregroundColor(Constants.detailInfoColor)

                Text("Games Played")
                  .font(Constants.detailFont)
                  .foregroundColor(Constants.detailColor)
              }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
          }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
      }
      .frame(maxWidth: .infinity, alignment: .leading)

      if let bio = accountInfo?.bio {
        Text("This is a sample bio blah blah blah\nThis is second line of the bio hahaha")
          .font(Constants.bioFont)
          .foregroundColor(Constants.bioColor)
          .padding(.top, 8)
      }

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
      } else if showEditProfileButton {
        Button {
          delegate?.editProfilePressed()
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
      }

      Rectangle()
        .fill(Constants.dividerColor)
        .frame(height: .one)
    }
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

extension UserInfoView {
  enum Constants {
    static let verticalSpacing: CGFloat = 16
    static let horizontalPadding: CGFloat = 16
    static let imageSize: CGFloat = 76
    static let usernameFont = YralFont.pt14.semiBold.swiftUIFont
    static let usernameColor =  YralColor.grey50.swiftUIColor
    static let detailInfoFont = YralFont.pt16.semiBold.swiftUIFont
    static let detailInfoColor = YralColor.grey50.swiftUIColor
    static let detailFont = YralFont.pt14.regular.swiftUIFont
    static let detailColor = YralColor.grey50.swiftUIColor
    static let bioFont = YralFont.pt12.regular.swiftUIFont
    static let bioColor = YralColor.grey50.swiftUIColor
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
  }
}

protocol UserInfoViewProtocol: Any {
  func loginPressed()
  func editProfilePressed()
}
