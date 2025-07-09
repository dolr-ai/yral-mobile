//
//  SignupView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI
import iosSharedUmbrella

struct SignupView: View {
  var delegate: SignupViewProtocol?
  @Binding var loadingProvider: SocialProvider?

  var body: some View {
    VStack(alignment: .center, spacing: .zero) {
      Image(Constants.headerImage)
        .resizable()
        .scaledToFit()
        .frame(width: Constants.headerImageWidth, height: Constants.headerImageHeight)
      VStack(alignment: .center, spacing: Constants.signupMessageVStackSpacing) {
        Text(Constants.signupMessageTitle)
          .font(Constants.titleFont)
          .foregroundStyle(Constants.vStackTextColor)
        Text(Constants.signupMessageSubtitle)
          .font(Constants.subtitleFont)
          .foregroundStyle(Constants.vStackTextColor)
      }
      .padding(.top, Constants.signupVStackTopPadding)
      Button {
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: SignupJourneySelected(
            authJourney: .google
          )
        )
        delegate?.signupwithGoogle()
      } label: {
        Group {
          if loadingProvider == .google {
            LottieLoaderView(animationName: Constants.lottieName)
              .frame(width: Constants.loaderSize, height: Constants.loaderSize)
          } else {
            HStack(spacing: Constants.buttonHStackSpacing) {
              Image(Constants.googleSignupImage)
                .resizable()
                .scaledToFit()
                .frame(width: Constants.iconSize, height: Constants.iconSize)
              Text(Constants.googleButtonText)
                .font(Constants.buttonFont)
                .foregroundStyle(Constants.buttonTextColor)
            }
          }
        }
        .frame(maxWidth: .infinity)
      }
      .disabled(loadingProvider != nil)
      .frame(maxWidth: .infinity)
      .frame(height: Constants.buttonHeight)
      .background(Constants.buttonBgColor)
      .cornerRadius(Constants.buttonCornerRadius)
      .padding(.top, Constants.googleButtonTopPadding)

      Button {
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: SignupJourneySelected(
            authJourney: .apple
          )
        )
        delegate?.signupwithApple()
      } label: {
        Group {
          if loadingProvider == .apple {
            LottieLoaderView(animationName: Constants.lottieName)
              .frame(width: Constants.loaderSize, height: Constants.loaderSize)
          } else {
            HStack(spacing: Constants.buttonHStackSpacing) {
              Image(Constants.appleSignupImage)
                .resizable()
                .scaledToFit()
                .frame(width: Constants.iconSize, height: Constants.iconSize)
              Text(Constants.appleButtonText)
                .font(Constants.buttonFont)
                .foregroundStyle(Constants.buttonTextColor)
            }
          }
        }
        .frame(maxWidth: .infinity)
      }
      .disabled(loadingProvider != nil)
      .frame(maxWidth: .infinity)
      .frame(height: Constants.buttonHeight)
      .background(Constants.buttonBgColor)
      .cornerRadius(Constants.buttonCornerRadius)
      .padding(.top, Constants.appleButtonTopPadding)

      HStack(alignment: .top, spacing: .zero) {
        Text(Constants.tncText)
          .font(Constants.tncTextFont)
          .foregroundStyle(Constants.tncTextColor)

        Link(Constants.tncLinkText, destination: Constants.termsURL)
          .font(Constants.tncLinkFont)
          .foregroundStyle(Constants.tncLinkTextColor)
      }
      .padding(.top, Constants.tncTopPadding)
    }
    .padding(.horizontal, Constants.horizontalPadding)
  }
}

extension SignupView {
  enum Constants {
    static let headerImage = "yral_login_header"
    static let googleSignupImage = "google_signup"
    static let appleSignupImage = "apple_signup"
    static let lottieName = "Yral_Loader"

    static let buttonBgColor = YralColor.grey0.swiftUIColor
    static let signupMessageTitle = "Continue to sign up for free"
    static let signupMessageSubtitle = "If you already have an account, we’ll log you in"
    static let googleButtonText = "Sign up with Google"
    static let appleButtonText = "Sign up with Apple"
    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let subtitleFont = YralFont.pt14.swiftUIFont
    static let buttonFont = YralFont.pt16.bold.swiftUIFont
    static let buttonTextColor = YralColor.grey900.swiftUIColor
    static let tncText = "By signing up, you agree to our "
    static let tncLinkText = "Terms of Service"
    static let termsURL = URL(string: "https://yral.com/terms-ios") ?? URL(fileURLWithPath: "")
    static let tncTextFont = YralFont.pt14.swiftUIFont
    static let tncLinkFont = YralFont.pt14.bold.swiftUIFont
    static let vStackTextColor = YralColor.grey0.swiftUIColor
    static let tncTextColor = YralColor.grey0.swiftUIColor
    static let tncLinkTextColor = YralColor.primary300.swiftUIColor

    static let horizontalPadding = 28.0
    static let buttonHStackSpacing = 12.0
    static let googleButtonTopPadding = 28.0
    static let appleButtonTopPadding = 24.0
    static let tncTopPadding = 20.0
    static let signupMessageVStackSpacing = 8.0
    static let signupVStackTopPadding = 46.0

    static let headerImageWidth = 240.0
    static let headerImageHeight = 86.0
    static let buttonHeight = 46.0
    static let loaderSize = 24.0
    static let iconSize = 16.0
    static let buttonCornerRadius = 8.0
  }
}

protocol SignupViewProtocol: Any {
  func signupwithGoogle()
  func signupwithApple()
}
