//
//  ProfileEmptyState.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

struct ProfileEmptyStateView: View {
  @EnvironmentObject var session: SessionManager

  @State private var isLoggedIn = false

  var loginPressed: (() -> Void)
  var createAIVideoPressed: (() -> Void)

  var body: some View {
    VStack(spacing: .zero) {
      Spacer(minLength: isLoggedIn ? Constants.createVideoSpacer : Constants.loginSpacer)

      Text(isLoggedIn ? Constants.createVideo : Constants.login)
        .font(Constants.titleFont)
        .foregroundColor(Constants.titleColor)

      Text(isLoggedIn ? Constants.createVideoSubtitle : Constants.loginSubtitle)
        .font(Constants.subtitleFont)
        .foregroundColor(Constants.subtitleColor)
        .multilineTextAlignment(.center)
        .padding(.top, Constants.subtitleTop)
        .padding(.bottom, Constants.subtitleBottom)

      Button {
        if isLoggedIn {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: UploadVideoClickedEventData(pageName: "profile")
          )
          createAIVideoPressed()
        } else {
          loginPressed()
        }
      }
      label: {
        Text(isLoggedIn ? Constants.createVideoButton : Constants.loginButton)
          .foregroundColor(Constants.buttonColor)
          .font(Constants.buttonFont)
          .frame(width: Constants.buttonWidth, height: Constants.buttonHeight)
          .background(Constants.buttonGradient)
          .cornerRadius(Constants.buttonCornerRadius)
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onReceive(session.phasePublisher) { phase in
      switch phase {
      case .permanent:
        isLoggedIn = true
      default:
        isLoggedIn = false
      }
    }
  }
}

extension ProfileEmptyStateView {
  enum Constants {
    static let loginSpacer = 140.0
    static let login = "Unlock Profile"

    static let createVideoSpacer = 140.0
    static let createVideo = "Create AI Video"

    static let loginSubtitle = "Log in to claim rewards, build your profile,\nand join the YRAL community"
    static let createVideoSubtitle = "You don't get the hype till you make\nyour own AI video."

    static let titleFont = YralFont.pt16.semiBold.swiftUIFont
    static let titleColor = YralColor.grey50.swiftUIColor

    static let subtitleTop = 10.0
    static let subtitleBottom = 30.0
    static let subtitleFont = YralFont.pt14.regular.swiftUIFont
    static let subtitleColor = YralColor.grey400.swiftUIColor

    static let loginButton = "Login"
    static let createVideoButton = "Create AI Video"
    static let buttonHeight = 44.0
    static let buttonWidth = 272.0
    static let buttonCornerRadius = 8.0
    static let buttonFont = YralFont.pt16.bold.swiftUIFont
    static let buttonColor = YralColor.grey50.swiftUIColor

    static let buttonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
  }
}
