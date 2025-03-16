//
//  CheckBoxAnimationView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 27/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct AnimatedCheckbox: View {
  @State private var scale: CGFloat = .zero
  @State private var opacity: Double = .zero
  let animationCompleted: () -> Void
  var body: some View {
    ZStack {
      Color.black.edgesIgnoringSafeArea(.all)
      Image(Constants.imageName)
        .resizable()
        .aspectRatio(contentMode: .fit)
        .frame(width: Constants.imageWidth, height: Constants.imageHeight)
        .scaleEffect(scale)
        .opacity(opacity)
        .onAppear {
          DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
            animateCheckbox()
          }
        }
    }
  }

  func animateCheckbox() {
    scale = .pointOne
    opacity = CGFloat.pointOne

    withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
      scale = Constants.keyFrame1
      opacity = CGFloat.one
    }

    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.keyFrameDuration) {
      withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
        scale = Constants.keyFrame2
        opacity = Constants.keyFrame2
      }
      DispatchQueue.main.asyncAfter(deadline: .now() + Constants.keyFrameDuration) {
        withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
          scale = Constants.keyFrame3
          opacity = CGFloat.one
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + Constants.keyFrameDuration) {
          withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
            scale = Constants.keyFrame4
            opacity = Constants.keyFrame4
          }
          DispatchQueue.main.asyncAfter(deadline: .now() + Constants.keyFrameDuration) {
            withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
              scale = Constants.keyFrame5
              opacity = CGFloat.one
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + Constants.keyFrameDuration) {
              withAnimation(Animation.easeInOut(duration: Constants.keyFrameDuration)) {
                scale = .one
                opacity = CGFloat.one
                animationCompleted()
              }
            }
          }
        }
      }
    }
  }
}

extension AnimatedCheckbox {
  enum Constants {
    static let keyFrameDuration = 0.18
    static let imageName = "upload_succes_tick"
    static let imageWidth = 120.0
    static let imageHeight = 133.0
    static let keyFrame1 = 1.5
    static let keyFrame2 = 0.9
    static let keyFrame3 = 1.08
    static let keyFrame4 = 0.95
    static let keyFrame5 = 1.05
  }
}
