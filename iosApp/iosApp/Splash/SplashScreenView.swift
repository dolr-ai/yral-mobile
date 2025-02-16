//
//  SplashScreen.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct SplashScreenView: View {
  var body: some View {
    ZStack {
      Color.white.ignoresSafeArea()
      LottieView(name: Constants.splashAnimation,
                 loopMode: .playOnce,
                 animationSpeed: .one)
      .ignoresSafeArea()
      .background(.black)
      .edgesIgnoringSafeArea(.all)
    }
  }
}

extension SplashScreenView {
  enum Constants {
    static let splashAnimation = "Splash_Lottie"
  }
}
