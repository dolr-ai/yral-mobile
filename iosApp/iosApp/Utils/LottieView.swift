//
//  LottieView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Lottie

struct LottieView: UIViewRepresentable {
  let name: String
  let loopMode: LottieLoopMode
  let animationSpeed: CGFloat
  let animationCompleted: () -> Void

  func makeUIView(context: Context) -> UIView {
    let containerView = UIView(frame: .zero)
    let animationView = LottieAnimationView()
    animationView.animation = LottieAnimation.named(name)
    animationView.animationSpeed = animationSpeed
    animationView.loopMode = loopMode
    animationView.contentMode = .scaleAspectFill
    animationView.translatesAutoresizingMaskIntoConstraints = false
    containerView.addSubview(animationView)

    NSLayoutConstraint.activate([
      animationView.topAnchor.constraint(equalTo: containerView.topAnchor),
      animationView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),
      animationView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
      animationView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor)
    ])

    animationView.play { finished in
      if finished && loopMode == .playOnce {
        animationView.currentProgress = .zero
      }
      animationCompleted()
    }
    return containerView
  }

  func updateUIView(_ uiView: UIView, context: Context) {
    // If needed, handle updates here.
  }
}
