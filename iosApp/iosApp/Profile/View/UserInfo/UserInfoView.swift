//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UserInfoView: View {
  @Binding var profileInfo: ProfileInfo?

  var body: some View {
    VStack(spacing: Constants.verticalSpacing) {
      Spacer().frame(width: Constants.verticalSpacing)
      HStack(spacing: Constants.horizontalSpacing) {
        URLImage(url: profileInfo?.imageURL)
          .frame(
            width: Constants.imageSize,
            height: Constants.imageSize
          )
        Text(profileInfo?.canisterID ?? "")
          .font(Constants.profileLabelFont)
          .foregroundColor(Constants.profileLabelColor)
          .lineLimit(.two)
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      VStack(alignment: .leading, spacing: Constants.secondaryVStackSpacing) {
        Button { }
        label: {
          Text(Constants.loginButtonTitle)
            .foregroundColor(Constants.loginButtonTextColor)
            .font(Constants.loginButtonFont)
            .frame(maxWidth: .infinity, minHeight: Constants.loginButtonHeight)
            .background(Constants.loginButtonGradient)
            .cornerRadius(Constants.loginButtonCornerRadius)
        }
        Text(Constants.loginMessageString)
          .foregroundColor(Constants.loginLabelColor)
          .font(Constants.loginLabelFont)
      }
      Rectangle()
        .fill(Constants.dividerColor)
        .frame(height: .one)
    }
    .padding([.horizontal], Constants.horizontalPadding)
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

#Preview {
  @State var info: ProfileInfo? = ProfileInfo(imageURL: URL(fileURLWithPath: ""), canisterID: "")
  UserInfoView(profileInfo: $info)
}

extension UserInfoView {
  enum Constants {
    static let verticalSpacing: CGFloat = 30
    static let horizontalSpacing: CGFloat = 16
    static let horizontalPadding: CGFloat = 16
    static let imageSize: CGFloat = 60
    static let profileLabelFont = Font
      .custom("Kumbh Sans", fixedSize: 14)
      .weight(.medium)
    static let profileLabelColor =  Color(red: 0.64, green: 0.64, blue: 0.64)
    static let secondaryVStackSpacing = 16.0
    static let loginButtonTitle = "Login"
    static let loginButtonTextColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let loginButtonFont = Font.custom("Kumbh Sans", size: 16)
      .weight(.bold)
    static let loginMessageString = "Your Yral account has been setup. Login with Google to not lose progress."
    static let loginLabelColor = Color.white
    static let loginLabelFont = Font.custom("Kumbh Sans", size: 12)
      .weight(.medium)
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
    static let dividerColor = Color(red: 0.14, green: 0.14, blue: 0.14)
  }
}
