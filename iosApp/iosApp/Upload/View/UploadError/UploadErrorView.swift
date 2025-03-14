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

  private var descriptionAttributedText: NSAttributedString {
    let paragraphStyle = NSMutableParagraphStyle()
    paragraphStyle.lineHeightMultiple = Constants.descriptionLineHeightMultiplier
    let baseFont = Constants.descriptionFont
    let attributed = NSMutableAttributedString()
    let attrs: [NSAttributedString.Key: Any] = [
      .font: baseFont,
      .foregroundColor: Constants.descriptionColor,
      .paragraphStyle: paragraphStyle
    ]
    attributed.append(NSAttributedString(string: Constants.descriptionText, attributes: attrs))
    return attributed
  }

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
          AttributedText(
            attributedString: descriptionAttributedText,
            horizontalPadding: Constants.horizontalPadding,
            alignment: .center
          )
          .frame(height: Constants.descriptionHeight)
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
            .overlay(
              Constants.buttonGradient
                .mask(Text(Constants.goHomeButtonText).font(Constants.goHomeButtonFont))
            )
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

    static let descriptionText = "We couldn’t upload your video due to some reasons. Try uploading it again."
    static let descriptionFont = UIFont(name: "Kumbh Sans", size: 16)
    ?? UIFont.systemFont(ofSize: 16)
    static let descriptionColor =  UIColor(red: 0.83, green: 0.83, blue: 0.83, alpha: 1.0)
    static let descriptionLineHeightMultiplier = 1.13
    static let descriptionHorizontalPadding: CGFloat = 32
    static let descriptionHeight = 66.0

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
    static let buttonGradient = LinearGradient(
      gradient: Gradient(stops: [
        .init(color: Color(red: 1.0, green: 120/255, blue: 193/255), location: 0.0),
        .init(color: Color(red: 226/255, green: 1/255, blue: 123/255), location: 0.5),
        .init(color: Color(red: 173/255, green: 0, blue: 94/255), location: 1.0)
      ]),
      startPoint: .topTrailing,
      endPoint: .topLeading
    )
  }
}
