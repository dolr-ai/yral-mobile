//
//  UploadSuccessView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct UploadCompletedView: View {
  var doneAction: () -> Void
  @Binding var showUploadCompletedView: Bool
  @State var animated = false
  @Environment(\.dismiss) private var dismiss

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
    ZStack {
      Constants.backgroundColor
        .ignoresSafeArea()
      VStack(spacing: Constants.vStackSpacing) {
        AnimatedCheckbox {
          animated = true
        }
        .frame(width: Constants.iconSizeWidth, height: Constants.iconSizeHeight)
        if animated {
          VStack(spacing: Constants.textVStackSpacing) {
            Text(Constants.titleText)
              .font(Constants.titleFont)
              .foregroundColor(Constants.titleColor)

            AttributedText(
              attributedString: processingAttributedText,
              horizontalPadding: Constants.vStackPadding,
              alignment: .center
            )
            .frame(height: Constants.descriptionHeight)
          }
          Button(action: {
            doneAction()
            dismiss()
            withAnimation {
              showUploadCompletedView = false
            }
          }, label: {
            Text(Constants.buttonTitle)
              .font(Constants.buttonFont)
              .overlay(
                Constants.buttonGradient
                  .mask(Text(Constants.buttonTitle).font(Constants.buttonFont))
              )
          })
          .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
        }
      }
      .padding(.horizontal, Constants.vStackPadding)
    }
    .onDisappear {
      UIView.setAnimationsEnabled(true)
    }
  }
}

extension UploadCompletedView {
  enum Constants {
    static let backgroundColor: Color = .black

    static let uploadSuccessImage = "upload_succes_tick"
    static let iconSizeWidth: CGFloat = 120.0
    static let iconSizeHeight: CGFloat = 133.0
    static let iconBottomPadding: CGFloat = 16

    static let titleText = "Upload Completed"
    static let titleColor: Color = YralColor.grey0.swiftUIColor
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let descriptionText1 = "We’re processing your video. It’ll be in "
    static let descriptionText2 = "\n'Your Videos'"
    static let descriptionText3 = " under "
    static let descriptionText4 = "My Profile"
    static let descriptionText5 = " soon."
    static let descriptionText6 = "\nHappy scrolling!"
    static let descriptionColor =  YralColor.grey300.uiColor
    static let descriptionBaseFont = YralFont.pt16.uiFont
    static let descriptionBoldFont = YralFont.pt16.bold.uiFont
    static let descriptionHeight = 66.0

    static let descriptionLineHeight = 1.13
    static let buttonTitle = "Done"
    static let buttonHeight: CGFloat = 45
    static let buttonFont = YralFont.pt18.bold.swiftUIFont
    static let buttonGradient = LinearGradient(
      gradient: Gradient(stops: [
        .init(color: Color(red: 1.0, green: 120/255, blue: 193/255), location: 0.0),
        .init(color: Color(red: 226/255, green: 1/255, blue: 123/255), location: 0.5),
        .init(color: Color(red: 173/255, green: 0, blue: 94/255), location: 1.0)
      ]),
      startPoint: .topTrailing,
      endPoint: .topLeading
    )

    static let vStackSpacing: CGFloat = 24
    static let vStackPadding: CGFloat = 36
    static let textVStackSpacing: CGFloat = 8
  }
}
