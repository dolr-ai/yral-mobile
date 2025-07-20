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
  let loopMode: LottieLoopMode = .loop
  let animationSpeed: CGFloat = .one

  var body: some View {
    LottieView(name: animationName,
               loopMode: loopMode,
               animationSpeed: animationSpeed,
               contentMode: .scaleAspectFill,
               animationCompleted: {
    })
    .frame(maxWidth: .infinity, maxHeight: .infinity)
  }
}
