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
  var onFocus: (() -> Void)?
  @FocusState private var isCaptionFocused: Bool

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      Text(Constants.captionText)
        .foregroundColor(Constants.textColor)
        .font(Constants.font)
        .background(Color.clear)
      ZStack(alignment: .topLeading) {
        if #available(iOS 16.0, *) {
          TextEditor(text: $caption)
            .focused($isCaptionFocused)
            .scrollContentBackground(.hidden)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.textColor)
            .font(Constants.font)
            .frame(minHeight: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(
                  isCaptionFocused ? Constants.textEditorStrokeColorSelected :
                    Constants.textEditorStrokeColorUnselected,
                  lineWidth: CGFloat.one
                )
            )
            .onTapGesture {
              isCaptionFocused = true
              onFocus?()
            }
        } else {
          TextEditor(text: $caption)
            .focused($isCaptionFocused)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.textColor)
            .font(Constants.font)
            .frame(minHeight: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(
                  isCaptionFocused ? Constants.textEditorStrokeColorSelected :
                    Constants.textEditorStrokeColorUnselected,
                  lineWidth: CGFloat.one
                )
            )
            .onTapGesture {
              isCaptionFocused = true
              onFocus?()
            }
        }
        if caption.isEmpty {
          Text(Constants.textFieldPlaceholder)
            .foregroundColor(Constants.placeholderTextColor)
            .font(Constants.font)
            .padding(Constants.placeholderPadding)
        }
      }
    }
    .onAppear {
      UITextView.appearance().tintColor = Constants.tintColor
    }
  }
}

extension CaptionsView {
  enum Constants {
    static let textColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let textEditorBackgroundColor = Color(red: 0.09, green: 0.09, blue: 0.09)
    static let textEditorStrokeColorUnselected = Color(red: 0.13, green: 0.13, blue: 0.13)
    static let textEditorStrokeColorSelected = Color(red: 0.64, green: 0.64, blue: 0.64)
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
    static let tintColor: UIColor =  UIColor(red: 0.89, green: 0, blue: 0.48, alpha: 1.0)
  }
}
