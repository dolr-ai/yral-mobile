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
  let data: Data?
  let name: String
  let loopMode: LottieLoopMode
  let animationSpeed: CGFloat
  let resetProgress: Bool
  let animationCompleted: () -> Void

  init(
    data: Data? = nil,
    name: String,
    loopMode: LottieLoopMode,
    animationSpeed: CGFloat,
    resetProgress: Bool,
    animationCompleted: @escaping () -> Void
  ) {
    self.data = data
    self.name = name
    self.loopMode = loopMode
    self.animationSpeed = animationSpeed
    self.resetProgress = resetProgress
    self.animationCompleted = animationCompleted
  }

  func makeUIView(context: Context) -> UIView {
    let containerView = UIView(frame: .zero)
    let animationView = LottieAnimationView()
    animationView.animation = LottieAnimation.named(name)
    if let animationData = data {
      animationView.animation = try? LottieAnimation.from(data: animationData)
    }
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
      if finished && loopMode == .playOnce && resetProgress {
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
