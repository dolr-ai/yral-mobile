//
//  GameToggleAnimationView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 20/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameToggleAnimationView: View {
  @Binding var animationVisible: Bool
  var gameType: FeedGame

  var body: some View {
    ZStack {
      Constants.backgroundColor
        .opacity(Constants.backgroundColorOpacity)

      LottieView(
        name: gameType == .hon ? Constants.honGameLottie : Constants.smileyGameLottie,
        loopMode: .playOnce,
        animationSpeed: .one,
        contentMode: .scaleAspectFit) {
          DispatchQueue.main.asyncAfter(deadline: .now() + Constants.animationEndDelay) {
            animationVisible = false
          }
        }
    }
  }
}

extension GameToggleAnimationView {
  enum Constants {
    static let backgroundColor = Color.black
    static let backgroundColorOpacity = 0.8
    static let animationEndDelay = 0.5
    static let smileyGameLottie = "toggle_smiley_game"
    static let honGameLottie = "toggle_hon_game"
  }
}
