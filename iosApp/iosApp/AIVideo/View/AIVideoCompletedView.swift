//
//  AIVideoCompletedView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct AIVideoCompletedView: View {
  let videoURL: URL
  let videoAspectRatio: CGFloat
  let onDone: () -> Void

  private var processingAttributedText: NSAttributedString {
    let paragraphStyle = NSMutableParagraphStyle()
    paragraphStyle.lineHeightMultiple = Constants.descriptionLineHeight

    let baseFont = Constants.descriptionBaseFont
    let boldFont = Constants.descriptionBoldFont

    let attributed = NSMutableAttributedString()

    let attrs: [NSAttributedString.Key: Any] = [
      .font: baseFont,
      .foregroundColor: Constants.descriptionColor,
      .paragraphStyle: paragraphStyle
    ]

    let boldAttrs: [NSAttributedString.Key: Any] = [
      .font: boldFont,
      .foregroundColor: Constants.descriptionColor,
      .paragraphStyle: paragraphStyle
    ]

    attributed.append(NSAttributedString(string: Constants.descriptionText1, attributes: attrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText2, attributes: boldAttrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText3, attributes: attrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText4, attributes: boldAttrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText5, attributes: attrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText6, attributes: attrs))

    return attributed
  }

  var body: some View {
    VStack(spacing: .zero) {
      ResizableVideoView(
        url: videoURL,
        aspectRatio: videoAspectRatio
      )
      .frame(
        width: videoAspectRatio == Constants.verticalVideoAspectRatio
        ? Constants.verticalVideoWidth : Constants.horizontalVideoWidth
      )

      Text(Constants.title)
        .font(Constants.titleFont)
        .foregroundColor(Constants.titleColor)
        .padding(.top, Constants.titleTop)
        .padding(.bottom, Constants.titleBottom)

      AttributedText(
        attributedString: processingAttributedText,
        alignment: .center
      )
      .frame(height: Constants.descriptionHeight)

      Button {
        onDone()
      } label: {
        Text(Constants.doneButtonText)
          .font(Constants.doneButtonTextFont)
          .foregroundColor(Constants.doneButtonTextColor)
          .frame(maxWidth: .infinity)
          .frame(height: Constants.doneButtonHeight)
          .background(Constants.doneButtonGradient)
          .cornerRadius(Constants.doneButtonCornerRadius)
      }
      .padding(.top, Constants.doneButtonTop)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    .padding(.horizontal, Constants.vstackHorizontal)
    .padding(.vertical, Constants.vstackVertical)
    .background(Constants.vstackBackground)
  }
}

extension AIVideoCompletedView {
  enum Constants {
    static let vstackHorizontal = 40.0
    static let vstackVertical = 56.0
    static let vstackBackground = YralColor.grey950.swiftUIColor

    static let verticalVideoWidth = 150.0
    static let horizontalVideoWidth = 300.0
    static let verticalVideoAspectRatio: CGFloat = 9/16

    static let title = "Upload Successful"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleBottom = 8.0
    static let titleTop = 40.0

    static let descriptionBaseFont = YralFont.pt16.regular.uiFont
    static let descriptionBoldFont = YralFont.pt16.bold.uiFont
    static let descriptionText1 = "We’re processing your video. It’ll be in "
    static let descriptionText2 = "\n'Your Videos'"
    static let descriptionText3 = " under "
    static let descriptionText4 = "My Profile"
    static let descriptionText5 = " soon."
    static let descriptionText6 = "\nHappy scrolling!"
    static let descriptionColor =  YralColor.grey50.uiColor
    static let descriptionLineHeight = 1.13
    static let descriptionHeight = 66.0

    static let doneButtonText = "Done"
    static let doneButtonTextFont = YralFont.pt16.medium.swiftUIFont
    static let doneButtonTextColor = YralColor.grey50.swiftUIColor
    static let doneButtonCornerRadius = 8.0
    static let doneButtonHeight = 42.0
    static let doneButtonTop = 24.0
    static let doneButtonGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
      ],
      startPoint: .init(x: 0.94, y: 0.13),
      endPoint: .init(x: 0.35, y: 0.89)
    )
  }
}
