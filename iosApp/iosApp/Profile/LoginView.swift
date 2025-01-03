//
//  LoginView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct LoginView: View {
  // Sample menu items
  let menuItems = [
    (iconName: LoginConstants.teamIconName, title: "Talk to the Team"),
    (iconName: LoginConstants.termsIconName, title: "Terms of Service"),
    (iconName: LoginConstants.privacyIconName, title: "Privacy Policy")
  ]

  var body: some View {
    ZStack(alignment: .bottom) {
      LoginConstants.backgroundColor
        .ignoresSafeArea()

      VStack(spacing: 0) {
        ScrollView(showsIndicators: false) {
          VStack(spacing: LoginConstants.verticalSpacing) {
            // Top padding
            Spacer()
              .frame(height: LoginConstants.topPadding)

            // Profile image + user ID
            ProfileHeaderView(
              profileImageName: LoginConstants.profileImageName,
              userId: "mqxpy-vp4st-vhw6p-poxzk-i363n-y4fagmqxpy-vp4st"
            )

            // Login button
            LoginButton(title: "Login")
              .padding(.horizontal, LoginConstants.horizontalPadding)

            // Info text
            InfoTextView(text: "Your Yral account has been setup. Login with Google to not lose progress.")
              .padding(.horizontal, LoginConstants.horizontalPadding)

            Divider()
              .background(LoginConstants.primaryTextColor.opacity(0.3))
              .padding(.top, 24)

            // Menu Items
            VStack {
              ForEach(menuItems, id: \.title) { item in
                MenuRowView(iconName: item.iconName, title: item.title) {
                  // Action for row tap
                }
              }
            }
            .padding(.top, 8)

            // Follow us
            FollowUsView()
              .padding(.top, 32)

            // Powered by
            PoweredByView()
              .padding(.top, 24)

            // Extra bottom spacing
            Spacer().frame(height: 100)
          }
        }
      }
    }
    .foregroundColor(LoginConstants.primaryTextColor)
  }
}
