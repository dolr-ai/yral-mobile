//
//  CaptionsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct CaptionsView: View {
  @Binding var caption: String

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      Text(Constants.captionText)
        .foregroundColor(Constants.textColor)
        .font(Constants.font)
        .background(Color.clear)
      ZStack(alignment: .topLeading) {
        if #available(iOS 16.0, *) {
          TextEditor(text: $caption)
            .scrollContentBackground(.hidden)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.textColor)
            .font(Constants.font)
            .frame(minHeight: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(Constants.textEditorStrokeColor)
            )
        } else {
          TextEditor(text: $caption)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.textColor)
            .font(Constants.font)
            .frame(minHeight: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(Constants.textEditorStrokeColor)
            )
        }
        if caption.isEmpty {
          Text(Constants.textFieldPlaceholder)
            .foregroundColor(Constants.placeholderTextColor)
            .font(Constants.font)
            .padding(Constants.placeholderPadding)
        }
      }
    }
  }
}

extension CaptionsView {
  enum Constants {
    static let textColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let textEditorBackgroundColor =  Color(red: 0.09, green: 0.09, blue: 0.09)
    static let textEditorStrokeColor =   Color(red: 0.13, green: 0.13, blue: 0.13)
    static let placeholderTextColor =  Color(red: 0.32, green: 0.32, blue: 0.32)
    static let font = Font.custom("Kumbh Sans", size: 14)
    static let captionText = "Caption"
    static let textFieldPlaceholder = "Enter the caption here"
    static let textFieldHeight: CGFloat = 100
    static let textEditorRadius = 8.0
    static let vStackSpacing = 8.0
    static let placeholderPadding = EdgeInsets(top: 12.0, leading: 12.0, bottom: .zero, trailing: .zero)
    static let cursorPadding = EdgeInsets(top: 4.0, leading: 8.0, bottom: .zero, trailing: .zero)
    static let textPadding = 12.0
  }
}
