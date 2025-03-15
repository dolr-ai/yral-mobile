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
        .font(Constants.captionTextFont)
        .background(Color.clear)
      ZStack(alignment: .topLeading) {
        if #available(iOS 16.0, *) {
          TextEditor(text: $caption)
            .focused($isCaptionFocused)
            .scrollContentBackground(.hidden)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.textColor)
            .font(Constants.captionTextFieldFont)
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
            .font(Constants.captionTextFieldFont)
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
            .font(Constants.captionTextFieldFont)
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
    static let textColor = YralColor.grey50.swiftUIColor
    static let textEditorBackgroundColor = YralColor.grey900.swiftUIColor
    static let textEditorStrokeColorUnselected = YralColor.grey800.swiftUIColor
    static let textEditorStrokeColorSelected = YralColor.grey500.swiftUIColor
    static let placeholderTextColor =  YralColor.grey600.swiftUIColor
    static let captionTextFont = YralFont.pt14.medium.swiftUIFont
    static let captionText = "Caption"
    static let textFieldPlaceholder = "Enter the caption here"
    static let captionTextFieldFont = YralFont.pt14.swiftUIFont
    static let textFieldHeight: CGFloat = 100
    static let textEditorRadius = 8.0
    static let vStackSpacing = 8.0
    static let placeholderPadding = EdgeInsets(top: 12.0, leading: 12.0, bottom: .zero, trailing: .zero)
    static let cursorPadding = EdgeInsets(top: 4.0, leading: 8.0, bottom: .zero, trailing: .zero)
    static let textPadding = 12.0
    static let tintColor: UIColor =  YralColor.primary300.uiColor
  }
}
