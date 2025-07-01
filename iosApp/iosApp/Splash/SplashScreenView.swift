//
//  SplashScreen.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI
import iosSharedUmbrella

struct SplashScreenView: View {
  @State var isInitialAnimationComplete = false
  var body: some View {
    ZStack {
      Color.black.ignoresSafeArea()
      if isInitialAnimationComplete {
        LottieView(name: Constants.lightningAnimation,
                   loopMode: .loop,
                   animationSpeed: .one) {
        }
                   .ignoresSafeArea()
                   .background(.black)
                   .edgesIgnoringSafeArea(.all)
      } else {
        LottieView(name: Constants.splashAnimation,
                   loopMode: .playOnce,
                   animationSpeed: .one) {
          DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.one) {
            isInitialAnimationComplete = true
          }
        }
                   .ignoresSafeArea()
                   .background(.black)
                   .edgesIgnoringSafeArea(.all)
      }
    }
    .onAppear {
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(event: SplashScreenViewedEventData())
    }
  }
}

extension SplashScreenView {
  enum Constants {
    static let splashAnimation = "Splash_Lottie"
    static let lightningAnimation = "Lightning_Lottie"
  }
}
