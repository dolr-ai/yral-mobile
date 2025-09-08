//
//  LottieLoaderView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 31/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Lottie

struct LottieLoaderView: View {
  let animationName: String
  var loopMode: LottieLoopMode = .loop
  let animationSpeed: CGFloat = .one
  let resetProgess: Bool

  var body: some View {
    LottieView(name: animationName,
               loopMode: loopMode,
               animationSpeed: animationSpeed,
               resetProgress: resetProgess,
               animationCompleted: {
    })
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}
