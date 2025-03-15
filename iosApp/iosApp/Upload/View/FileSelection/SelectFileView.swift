//
//  SelectFileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SelectFileView: View {
  @Binding var showVideoPicker: Bool
  var body: some View {
    VStack(alignment: .center, spacing: Constants.outerVStackSpacing) {
      VStack(alignment: .center, spacing: Constants.innerVStackSpacing) {
        Text(Constants.uploadText)
          .font(Constants.uploadTextFont)
          .foregroundColor(Constants.uploadTextColor)
        Text(Constants.fileSizeText)
          .font(Constants.fileSizeFont)
          .foregroundColor(Constants.fileSizeColor)
      }
      Button { showVideoPicker = true }
      label: {
        Text(Constants.selectFileText)
          .font(Constants.selectFileFont)
          .foregroundColor(Constants.selectFileColor)
          .frame(width: Constants.selectFileButtonWidth, height: Constants.selectFileButtonHeight)
          .cornerRadius(Constants.selectFileButtonRadius)
          .overlay(
            RoundedRectangle(cornerRadius: Constants.selectFileButtonRadius)
              .stroke(Constants.selectFileColor, lineWidth: .one)
          )
      }
    }
    .frame(maxWidth: .infinity)
    .background(Color.black.edgesIgnoringSafeArea(.all))
    .frame(height: Constants.selectFileViewHeight)
    .overlay {
      RoundedRectangle(cornerRadius: Constants.outerVStackRadius)
        .stroke(Constants.outerVStackStrokeColor, lineWidth: .one)
    }
  }
}

extension SelectFileView {
  enum Constants {
    static let outerVStackSpacing = 20.0
    static let outerVStackRadius = 8.0
    static let outerVStackStrokeColor =  YralColor.grey800.swiftUIColor
    static let innerVStackSpacing = 10.0
    static let uploadText = "Upload a video to share with the world!"
    static let uploadTextFont = YralFont.pt16.medium.swiftUIFont
    static let uploadTextColor =  YralColor.grey50.swiftUIColor
    static let fileSizeText = "Video file ( Max 60s)"
    static let fileSizeFont = YralFont.pt12.medium.swiftUIFont
    static let fileSizeColor =  YralColor.grey500.swiftUIColor
    static let selectFileText = "Select File"
    static let selectFileFont = YralFont.pt14.semiBold.swiftUIFont
    static let selectFileColor = YralColor.primary300.swiftUIColor
    static let selectFileButtonRadius = 8.0
    static let selectFileButtonWidth = 107.0
    static let selectFileButtonHeight = 40.0
    static let selectFileViewHeight = 300.0
  }
}
