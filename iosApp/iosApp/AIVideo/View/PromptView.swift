//
//  PromptView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct PromptView: View {
  var onFocus: (() -> Void)?

  @Binding var prompt: String
  @FocusState private var isPromptFocused: Bool

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      Text(Constants.promptTitle)
        .foregroundColor(Constants.promptColor)
        .font(Constants.promptFont)
        .background(Color.clear)

      ZStack(alignment: .topLeading) {
        if #available(iOS 16.0, *) {
          TextEditor(text: $prompt)
            .focused($isPromptFocused)
            .scrollContentBackground(.hidden)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.promptColor)
            .font(Constants.captionTextFieldFont)
            .frame(minHeight: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(
                  isPromptFocused ? Constants.textEditorStrokeColorSelected :
                    Constants.textEditorStrokeColorUnselected,
                  lineWidth: CGFloat.one
                )
            )
            .onTapGesture {
              isPromptFocused = true
              onFocus?()
            }
        } else {
          TextEditor(text: $prompt)
            .focused($isPromptFocused)
            .padding(Constants.cursorPadding)
            .foregroundColor(Constants.promptColor)
            .font(Constants.captionTextFieldFont)
            .frame(height: Constants.textFieldHeight)
            .background(Constants.textEditorBackgroundColor)
            .cornerRadius(Constants.textEditorRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.textEditorRadius)
                .stroke(
                  isPromptFocused ? Constants.textEditorStrokeColorSelected :
                    Constants.textEditorStrokeColorUnselected,
                  lineWidth: CGFloat.one
                )
            )
            .onTapGesture {
              isPromptFocused = true
              onFocus?()
            }
        }
        if prompt.isEmpty {
          Text(Constants.textFieldPlaceholder)
            .foregroundColor(Constants.placeholderTextColor)
            .font(Constants.captionTextFieldFont)
            .padding(Constants.placeholderPadding)
        }
      }
      .frame(height: Constants.textFieldHeight)
    }
    .onAppear {
      UITextView.appearance().tintColor = Constants.tintColor
    }
  }
}

extension PromptView {
  enum Constants {
    static let vStackSpacing = 8.0

    static let promptTitle = "Prompt"
    static let promptFont = YralFont.pt14.medium.swiftUIFont
    static let promptColor = YralColor.grey300.swiftUIColor

    static let textEditorBackgroundColor = YralColor.grey900.swiftUIColor
    static let textEditorStrokeColorUnselected = YralColor.grey800.swiftUIColor
    static let textEditorStrokeColorSelected = YralColor.grey500.swiftUIColor
    static let placeholderTextColor =  YralColor.grey600.swiftUIColor
    static let textFieldPlaceholder = "Enter the Prompt here"
    static let captionTextFieldFont = YralFont.pt14.swiftUIFont
    static let textFieldHeight: CGFloat = 150
    static let textEditorRadius = 8.0
    static let placeholderPadding = EdgeInsets(top: 12.0, leading: 12.0, bottom: .zero, trailing: .zero)
    static let cursorPadding = EdgeInsets(top: 4.0, leading: 8.0, bottom: .zero, trailing: .zero)
    static let textPadding = 12.0
    static let tintColor: UIColor =  YralColor.primary300.uiColor
  }
}
