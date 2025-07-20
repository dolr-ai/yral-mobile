//
//  SignupOverlay.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SignupOverlay: View {
  let onLoginTap: (_ provider: SocialProvider) -> Void
  @State private var loadingProvider: SocialProvider?

  var body: some View {
    ZStack(alignment: .top) {
      Rectangle()
        .fill(.ultraThinMaterial)
        .environment(\.colorScheme, .dark)
        .ignoresSafeArea()
        .padding(-Constants.overshoot)
      VStack(spacing: .zero) {
        SignupView(
          delegate: self,
          loadingProvider: $loadingProvider
        )
          .padding(.top, Constants.topSpacing)
        Spacer()
        VStack(spacing: Constants.bottomVStackSpacing) {
          Text(Constants.scrollText)
            .foregroundColor(Constants.scrollTextColor)
            .font(Constants.scrollTextFont)
          LottieView(
            name: Constants.signupLottie,
            loopMode: .loop,
            animationSpeed: .one,
            contentMode: .scaleAspectFill) {

            }
            .frame(width: Constants.lottieWidth, height: Constants.lottieHeight)
        }
        .padding(.bottom, Constants.bottomSpacing)
      }
    }
  }
}

extension SignupOverlay: SignupViewProtocol {
  func signupwithGoogle() {
    loadingProvider = .google
    onLoginTap(.google)
  }

  func signupwithApple() {
    loadingProvider = .apple
    onLoginTap(.apple)
  }
}

extension SignupOverlay {
  enum Constants {
    static let overshoot: CGFloat = 15
    static let topSpacing = 146.0
    static let bottomSpacing = 44.0
    static let bottomVStackSpacing = 8.0
    static let lottieWidth = 42.0
    static let lottieHeight = 36.0

    static let scrollText = "Scroll To Next Video"
    static let scrollTextColor = YralColor.grey50.swiftUIColor
    static let scrollTextFont = YralFont.pt16.bold.swiftUIFont

    static let signupLottie = "Signup_scroll_lottie"
  }
}
