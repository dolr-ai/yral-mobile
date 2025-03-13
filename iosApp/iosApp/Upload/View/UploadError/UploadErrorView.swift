//
//  UploadErrorView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 02/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
struct UploadErrorView: View {
  @Binding var showUploadFailedView: Bool
  var tryAgainAction: () -> Void
  var goHomeAction: () -> Void
  @Environment(\.dismiss) private var dismiss

  var body: some View {
    ZStack {
      Constants.backgroundColor
        .ignoresSafeArea()
      VStack(spacing: Constants.vStackSpacing) {
        Image(Constants.errorIconName)
          .resizable()
          .scaledToFit()
          .frame(width: Constants.iconWidth, height: Constants.iconHeight)
        VStack(spacing: Constants.textVStackSpacing) {
          Text(Constants.titleText)
            .font(Constants.titleFont)
            .foregroundColor(Constants.titleColor)
          Text(Constants.descriptionText)
            .font(Constants.descriptionFont)
            .foregroundColor(Constants.descriptionColor)
            .multilineTextAlignment(.center)
        }

        Button {
          tryAgainAction()
        } label: {
          Text(Constants.tryAgainButtonTitle)
            .font(Constants.tryAgainButtonFont)
            .foregroundColor(Constants.tryAgainButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.tryAgainButtonHeight)
            .background(Constants.tryAgainButtonGradient)
            .cornerRadius(Constants.tryAgainButtonCornerRadius)
        }

        Button {
          goHomeAction()
          dismiss()
          withAnimation {
            showUploadFailedView = false
          }
        } label: {
          Text(Constants.goHomeButtonText)
            .font(Constants.goHomeButtonFont)
            .foregroundColor(Constants.goHomeButtonColor)
        }
        .padding(.top, Constants.goHomeButtonTopPadding)
      }
      .padding(.horizontal, Constants.horizontalPadding)
    }
    .onDisappear {
      UIView.setAnimationsEnabled(true)
    }
  }
}

// MARK: - Constants

extension UploadErrorView {
  enum Constants {
    static let backgroundColor = Color.black
    static let vStackSpacing: CGFloat = 30.0
    static let horizontalPadding: CGFloat = 36.0

    static let errorIconName = "upload_error_image"
    static let iconWidth: CGFloat = 120
    static let iconHeight: CGFloat = 120

    static let textVStackSpacing = 8.0
    static let titleText = "Something went wrong!"
    static let titleFont = Font.custom("Kumbh Sans", size: 18)
      .weight(.bold)
    static let titleColor = Color.white

    static let descriptionText = "We couldn’t upload your video due to XYZ reasons. Try uploading it again."
    static let descriptionFont = Font.custom("Kumbh Sans", size: 16)
    static let descriptionColor =  Color(red: 0.83, green: 0.83, blue: 0.83)
    static let descriptionHorizontalPadding: CGFloat = 32

    static let tryAgainButtonTitle = "Try Again"
    static let tryAgainButtonFont = Font.custom("Kumbh Sans", size: 16).weight(.bold)
    static let tryAgainButtonTextColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let tryAgainButtonHeight: CGFloat = 45.0
    static let tryAgainButtonCornerRadius: CGFloat = 8.0
    static let tryAgainButtonGradient = UploadView.Constants.uploadButtonEnabledGradient

    static let goHomeButtonText = "Go to Home"
    static let goHomeButtonFont = Font.custom("Kumbh Sans", size: 16).weight(.bold)
    static let goHomeButtonColor = Color(red: 1, green: 0.47, blue: 0.76)
    static let goHomeButtonTopPadding: CGFloat = 8.0
  }
}
