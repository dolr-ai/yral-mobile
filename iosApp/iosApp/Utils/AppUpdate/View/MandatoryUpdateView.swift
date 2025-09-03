//
//  MandatoryUpdateView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct MandatoryUpdateView: View {
  var body: some View {
    ZStack {
      Color.black.edgesIgnoringSafeArea(.all)
      VStack(alignment: .center, spacing: .zero) {
        Text(Constants.titleText)
          .font(Constants.titleFont)
          .foregroundStyle(Constants.titleTextColor)
          .padding(.top, Constants.verticalPadding)

        LottieLoaderView(animationName: Constants.lottieName, loopMode: .playOnce)
          .frame(width: Constants.imageSize, height: Constants.imageSize)
          .padding(.top, Constants.imagePadding)

        Text(Constants.subtitltText)
          .font(Constants.subtitleFont)
          .foregroundStyle(Constants.subtitleTextColor)
          .multilineTextAlignment(.center)
          .padding(.top, Constants.subtitlePadding)

        Button {
          AppUpdateHandler.redirectToAppStore()
        } label: {
          Text(Constants.updateButtonText)
            .font(Constants.updateButtonFont)
            .foregroundStyle(Constants.updateButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
            .background(Constants.updateButtonGradient)
            .cornerRadius(Constants.buttonCornerRadius)
        }
        .padding(.top, Constants.buttonPadding)
        .padding(.bottom, Constants.buttonPadding)
      }
      .padding(.horizontal, Constants.vStackHorizontalPadding)
    }
  }
}

extension MandatoryUpdateView {
  enum Constants {
    static let verticalPadding = 36.0
    static let imagePadding = 46.0
    static let imageSize = 134.0
    static let subtitlePadding = 46.0
    static let buttonPadding = 28.0
    static let vStackHorizontalPadding = 16.0

    static let illustrationSize: CGFloat = 134.0
    static let buttonHeight = 45.0
    static let buttonCornerRadius = 8.0
    static let buttonVStackSpacing = 16.0

    static let titleText = "App Update Required"
    static let subtitltText = "We have added new features & Fixed some bugs to make the experience seamless."
    static let updateButtonText = "Update App"

    static let lottieName = "app_update_animation"
    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let titleTextColor = YralColor.grey0.swiftUIColor
    static let subtitleFont = YralFont.pt14.swiftUIFont
    static let subtitleTextColor = YralColor.grey50.swiftUIColor
    static let updateButtonFont = YralFont.pt16.bold.swiftUIFont
    static let updateButtonTextColor = YralColor.grey50.swiftUIColor
    static let updateButtonGradient = LinearGradient(
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
