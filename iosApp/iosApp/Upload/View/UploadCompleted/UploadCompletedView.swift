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

            (
              Text(Constants.descriptionText1)
              + Text(Constants.descriptionText2).bold()
              + Text(Constants.descriptionText3)
              + Text(Constants.descriptionText4).bold()
              + Text(Constants.descriptionText5)
            )
            .font(Constants.descriptionFont)
            .multilineTextAlignment(.center)
            .foregroundColor(Constants.descriptionColor)
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
              .foregroundColor(Constants.buttonTextColor)
          })
          .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
        }
      }
      .padding(.horizontal, Constants.vStackPadding)
    }
  }
}

extension UploadCompletedView {
  enum Constants {
    static let backgroundColor: Color = .black

    static let lottieName = "upload_completed_lottie"

    static let uploadSuccessImage = "upload_succes_tick"
    static let iconSizeWidth: CGFloat = 180
    static let iconSizeHeight: CGFloat = 200
    static let iconBottomPadding: CGFloat = 16

    static let titleText = "Upload Completed"
    static let titleColor: Color = .white
    static let titleFont = Font.custom("Kumbh Sans", size: 18)
      .weight(.bold)
    static let descriptionText1 = "We’re processing your video. It’ll be in "
    static let descriptionText2 = "Your Videos"
    static let descriptionText3 = " under "
    static let descriptionText4 = "My Profile"
    static let descriptionText5 = " soon. Happy scrolling!"
    static let descriptionColor =  Color(red: 0.83, green: 0.83, blue: 0.83)
    static let descriptionFont =  Font.custom("Kumbh Sans", size: 16)

    static let buttonTitle = "Done"
    static let buttonTextColor = Color(red: 1, green: 0.47, blue: 0.76)
    static let buttonHeight: CGFloat = 45
    static let buttonFont = Font.custom("Kumbh Sans", size: 18)
      .weight(.bold)

    static let vStackSpacing: CGFloat = 24
    static let vStackPadding: CGFloat = 36
    static let textVStackSpacing: CGFloat = 8
  }
}
