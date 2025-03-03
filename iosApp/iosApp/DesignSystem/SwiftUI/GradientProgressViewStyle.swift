//
//  GradientProgressViewStyle.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct GradientProgressViewStyle: ProgressViewStyle {
  let gradient: LinearGradient
  let cornerRadius: CGFloat
  let height: CGFloat

  func makeBody(configuration: Configuration) -> some View {
    GeometryReader { geometry in
      ZStack(alignment: .leading) {
        RoundedRectangle(cornerRadius: cornerRadius)
          .fill(Color.gray.opacity(CGFloat.animationPeriod))
          .frame(height: height)

        RoundedRectangle(cornerRadius: cornerRadius)
          .fill(gradient)
          .frame(
            width: geometry.size.width * CGFloat(configuration.fractionCompleted ?? 0.0),
            height: height
          )
      }
    }
  }
}
