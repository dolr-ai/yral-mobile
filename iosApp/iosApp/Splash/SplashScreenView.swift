//
//  SplashScreen.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct SplashScreenView: View {
  @State var isInitialAnimationComplete = false
  var body: some View {
    ZStack {
      Color.black.ignoresSafeArea()
      if isInitialAnimationComplete {
        LottieView(name: Constants.lightningAnimation,
                   loopMode: .loop,
                   animationSpeed: .one,
                   contentMode: .scaleAspectFill) {
        }
                   .ignoresSafeArea()
                   .background(.black)
                   .edgesIgnoringSafeArea(.all)
      } else {
        LottieView(name: Constants.splashAnimation,
                   loopMode: .playOnce,
                   animationSpeed: .one,
                   contentMode: .scaleAspectFill) {
          DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.one) {
            isInitialAnimationComplete = true
          }
        }
                   .ignoresSafeArea()
                   .background(.black)
                   .edgesIgnoringSafeArea(.all)
      }
    }
  }
}

extension SplashScreenView {
  enum Constants {
    static let splashAnimation = "Splash_Lottie"
    static let lightningAnimation = "Lightning_Lottie"
  }
}
