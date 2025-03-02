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
    VStack(spacing: Constants.outerVStackSpacing) {
      Color.black.edgesIgnoringSafeArea(.all)
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
          .frame(maxWidth: .infinity, minHeight: Constants.selectFileButtonHeight)
          .cornerRadius(Constants.selectFileButtonRadius)
      }
    }.overlay {
      RoundedRectangle(cornerRadius: Constants.outerVStackRadius)
        .stroke(Constants.outerVStackStrokeColor, lineWidth: .one)
    }
  }
}

extension SelectFileView {
  enum Constants {
    static let outerVStackSpacing = 20.0
    static let outerVStackRadius = 8.0
    static let outerVStackStrokeColor =  Color(red: 0.13, green: 0.13, blue: 0.13)
    static let innerVStackSpacing = 10.0
    static let uploadText = "Upload a video to share with the world!"
    static let uploadTextFont = Font.custom("Kumbh Sans", size: 16)
      .weight(.medium)
    static let uploadTextColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let fileSizeText = "Video file ( Max 60s)"
    static let fileSizeFont = Font.custom("Kumbh Sans", size: 12)
      .weight(.medium)
    static let fileSizeColor =  Color(red: 0.64, green: 0.64, blue: 0.64)
    static let selectFileText = "Select File"
    static let selectFileFont = Font.custom("Kumbh Sans", size: 14)
      .weight(.semibold)
    static let selectFileColor = Color(red: 0.89, green: 0, blue: 0.48)
    static let selectFileButtonRadius = 8.0
    static let selectFileButtonHeight = 68.0

  }
}
