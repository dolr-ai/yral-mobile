//
//  GameToggleView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameToggleView: View {
  @State var selectedIndex: Int

  let toggleTapped: (Int) -> Void

  var body: some View {
    HStack(spacing: Constants.hStackSpacing) {
      ZStack {
        RoundedRectangle(cornerRadius: Constants.roundedCornerRadius)
          .fill(Constants.roundedBackgroundColor)
          .frame(width: Constants.roundedWidth, height: Constants.roundedHeight)
          .opacity(selectedIndex == .zero ? Constants.roundedVisibleOpacity : Constants.roundedHiddenOpacity)

        Image(.toggleHon)
          .resizable()
          .frame(width: Constants.imageSize, height: Constants.imageSize)
      }
      .onTapGesture {
        withAnimation {
          selectedIndex = .zero
          toggleTapped(selectedIndex)
        }
      }

      ZStack {
        RoundedRectangle(cornerRadius: Constants.roundedCornerRadius)
          .fill(Constants.roundedBackgroundColor)
          .frame(width: Constants.roundedWidth, height: Constants.roundedHeight)
          .opacity(selectedIndex == .one ? Constants.roundedVisibleOpacity : Constants.roundedHiddenOpacity)

        Image(.toggleSmiley)
          .resizable()
          .frame(width: Constants.imageSize, height: Constants.imageSize)
      }
      .onTapGesture {
        withAnimation {
          selectedIndex = .one
          toggleTapped(selectedIndex)
        }
      }
    }
    .frame(width: Constants.hStackWidth, height: Constants.hStackHeight)
    .background(Constants.hStackBackground)
    .clipShape(RoundedRectangle(cornerRadius: Constants.hStackCornerRadius))
  }
}

extension GameToggleView {
  enum Constants {
    static let hStackSpacing = 5.0
    static let hStackWidth = 106.0
    static let hStackHeight = 51.0
    static let hStackBackground = YralColor.grey800.swiftUIColor.opacity(0.4)
    static let hStackCornerRadius = 25.5

    static let roundedCornerRadius = 21.0
    static let roundedWidth = 46.0
    static let roundedHeight = 42.0
    static let roundedHiddenOpacity = 0.0
    static let roundedVisibleOpacity = 1.0
    static let roundedBackgroundColor = YralColor.grey800.swiftUIColor

    static let imageSize = 28.0
  }
}
