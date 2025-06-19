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
  var uploadVideoPressed: (() -> Void)
  var body: some View {
    VStack(spacing: Constants.verticalSpacing) {
      Image(Constants.iconName)
        .resizable()
        .scaledToFit()
        .frame(width: Constants.iconSize, height: Constants.iconSize)

      VStack(spacing: Constants.verticalSpacingInner) {
        Text(Constants.messageText)
          .font(Constants.messageFont)
          .foregroundColor(Constants.messageColor)
          .frame(width: Constants.width)
          .multilineTextAlignment(.center)

        Button(
          action: {
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: UploadVideoClickedEventData(pageName: "Profile")
            )
            uploadVideoPressed()
          },
          label: {
            Text(Constants.buttonTitle)
              .font(Constants.buttonFont)
              .overlay(
                Constants.buttonTextGradient
                  .mask {
                    Text(Constants.buttonTitle)
                      .font(Constants.buttonFont)
                  }
              )
              .frame(width: Constants.width, height: Constants.buttonHeight)
              .background(Constants.buttonBackgroundColor)
              .cornerRadius(Constants.buttonCornerRadius)

          }
        )
      }
    }
    .padding(Constants.padding)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}

extension ProfileEmptyStateView {
  enum Constants {
    static let width = 269.0
    static let padding: CGFloat = 16.0
    static let verticalSpacing: CGFloat = 50.0
    static let verticalSpacingInner: CGFloat = 30.0

    static let iconName = "profile_empty_state"
    static let iconSize: CGFloat = 100.0

    static let messageText = "You have not uploaded any videos yet."
    static let messageFont = YralFont.pt18.medium.swiftUIFont
    static let messageColor: Color = YralColor.grey0.swiftUIColor

    static let buttonTitle = "Upload Video"
    static let buttonFont = YralFont.pt16.bold.swiftUIFont
    static let buttonTextGradient = LinearGradient(
      gradient: Gradient(stops: [
        .init(color: Color(red: 1.0, green: 120/255, blue: 193/255), location: 0.0),
        .init(color: Color(red: 226/255, green: 1/255, blue: 123/255), location: 0.5),
        .init(color: Color(red: 173/255, green: 0, blue: 94/255), location: 1.0)
      ]),
      startPoint: .topTrailing,
      endPoint: .topLeading
    )
    static let buttonBackgroundColor: Color = YralColor.grey0.swiftUIColor
    static let buttonCornerRadius: CGFloat = 8.0
    static let buttonVerticalPadding: CGFloat = 12.0
    static let buttonHeight = 45.0

  }
}

#Preview {
  ProfileEmptyStateView {
  }
}
