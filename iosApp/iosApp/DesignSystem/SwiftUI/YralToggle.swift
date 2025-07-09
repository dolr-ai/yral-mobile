//
//  YralToggle.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct YralToggle: ToggleStyle {
  func makeBody(configuration: Configuration) -> some View {
    HStack {
      configuration.label
      ZStack(alignment: configuration.isOn ? .trailing : .leading) {
        RoundedRectangle(cornerRadius: Constants.cornerRadius)
          .fill(configuration.isOn ? Constants.onBackgroundColor : Constants.offBackgroundColor)
          .frame(width: Constants.width, height: Constants.height)

        Circle()
          .fill(Constants.fillColor)
          .frame(width: Constants.circleSize, height: Constants.circleSize)
          .padding(Constants.circlePadding)
          .shadow(radius: .one)
      }
      .onTapGesture {
        withAnimation(.easeInOut) {
          configuration.isOn.toggle()
        }
      }
    }
  }
}

extension YralToggle {
  enum Constants {
    static let cornerRadius = 20.0
    static let onBackgroundColor = YralColor.primary400.swiftUIColor
    static let offBackgroundColor = YralColor.grey600.swiftUIColor
    static let fillColor = YralColor.grey0.swiftUIColor

    static let width = 54.0
    static let height = 30.0
    static let circleSize = 24.0
    static let circlePadding = 2.0
  }
}
