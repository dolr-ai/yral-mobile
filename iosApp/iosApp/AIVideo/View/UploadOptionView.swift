//
//  UploadOptionView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 12/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UploadOptionView: View {
  @State private var isSelected: Bool = false

  let option: UploadOption
  let action: () -> Void

  var body: some View {
    VStack(spacing: Constants.vstackSpacing) {
      Image(option.imageName)
        .resizable()
        .frame(width: Constants.imageSize, height: Constants.imageSize)

      Text(option.title)
        .font(Constants.titleFont)
        .foregroundColor(Constants.titleColor)
        .padding(.top, Constants.titleTop)

      Text(option.subtitle)
        .font(Constants.subtitleFont)
        .foregroundColor(Constants.subtitleColor)

      Text(option.ctaText)
        .font(Constants.ctaFont)
        .foregroundColor(isSelected ? Constants.selectedCTATextColor : Constants.unselectedCTATextColor)
        .padding(.vertical, Constants.ctaVertical)
        .padding(.horizontal, Constants.ctaHorizontal)
        .background(isSelected ? Constants.selectedCTABackgroundColor : Constants.unselectedCTABackgroundColor)
        .cornerRadius(Constants.ctaCornerRadius)
        .overlay(
          RoundedRectangle(cornerRadius: Constants.ctaCornerRadius)
            .stroke(
              isSelected ? Constants.selectedCTABorderColor : Constants.unselectedCTABorderColor,
              lineWidth: Constants.ctaBorderWidth
            )
        )
        .padding(.top, Constants.ctaTop)
    }
    .padding(.vertical, Constants.vstackVertical)
    .padding(.horizontal, Constants.vstackHorizontal)
    .frame(maxWidth: .infinity)
    .background(isSelected ? Constants.selectedBackgroundColor : Constants.unselectedBackgroundColor)
    .cornerRadius(Constants.vstackCornerRadius)
    .overlay(
      RoundedRectangle(cornerRadius: Constants.vstackCornerRadius)
        .stroke(
          isSelected ? Constants.selectedBorderColor : Constants.unselectedBorderColor,
          lineWidth: Constants.vstackBorderWidth
        )
    )
    .onTapGesture {
      isSelected = true
      action()

      DispatchQueue.main.asyncAfter(deadline: .now() + Constants.disableSelectionTime) {
        isSelected = false
      }
    }
  }
}

extension UploadOptionView {
  enum Constants {
    static let selectedBorderColor = YralColor.primary300.swiftUIColor
    static let unselectedBorderColor = YralColor.grey700.swiftUIColor
    static let selectedBackgroundColor = Color(hex: "1F0011")
    static let unselectedBackgroundColor = YralColor.grey900.swiftUIColor

    static let selectedCTATextColor = YralColor.grey50.swiftUIColor
    static let unselectedCTATextColor = YralColor.primary300.swiftUIColor
    static let selectedCTABorderColor = YralColor.primary400.swiftUIColor
    static let unselectedCTABorderColor = YralColor.primary300.swiftUIColor
    static let selectedCTABackgroundColor = YralColor.primary400.swiftUIColor
    static let unselectedCTABackgroundColor = YralColor.grey900.swiftUIColor

    static let vstackSpacing = 4.0
    static let vstackVertical = 32.0
    static let vstackHorizontal = 40.0
    static let vstackCornerRadius = 8.0
    static let vstackBorderWidth = 1.0

    static let imageSize = 24.0

    static let titleFont = YralFont.pt16.bold.swiftUIFont
    static let titleColor = YralColor.grey50.swiftUIColor
    static let titleTop = 8.0

    static let subtitleFont = YralFont.pt12.medium.swiftUIFont
    static let subtitleColor = YralColor.grey400.swiftUIColor

    static let ctaFont = YralFont.pt14.semiBold.swiftUIFont
    static let ctaVertical = 10.0
    static let ctaHorizontal = 20.0
    static let ctaTop = 16.0
    static let ctaCornerRadius = 8.0
    static let ctaBorderWidth = 1.0

    static let disableSelectionTime = 2.0
  }
}
