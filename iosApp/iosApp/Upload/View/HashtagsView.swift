//
//  CaptionsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct HashtagView: View {
  @Binding var hashtags: [String]
  @State private var newHashtag: String = ""

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      Text(Constants.addHashtagText)
        .foregroundColor(Constants.addHashtagColor)
        .font(Constants.addHashtagFont)
        .background(Color.clear)
      ScrollView(.horizontal, showsIndicators: false) {
        HStack(spacing: Constants.chipSpacing) {
          ForEach(hashtags, id: \.self) { tag in
            HashtagChip(tag: tag)
          }
          TextField("", text: $newHashtag)
          .placeholder(when: newHashtag.isEmpty) {
            Text(Constants.placeholderText)
              .foregroundColor(Constants.placeholderColor)
              .font(Constants.enterFont)
          }
          .foregroundColor(Constants.enterColor)
          .font(Constants.enterFont)
          .autocapitalization(.none)
          .disableAutocorrection(true)
          .onSubmit {
            addNewHashtag()
          }
        }
        .padding(Constants.hStackPadding)
      }
      .background(Constants.hStackBGColor)
      .cornerRadius(Constants.hStackRadius)
      .overlay(
        RoundedRectangle(cornerRadius: Constants.hStackRadius)
          .stroke(Constants.strokeColor, lineWidth: .one)
      )
    }
  }

  private func addNewHashtag() {
    let trimmed = newHashtag.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return }
    hashtags.append(trimmed)
    newHashtag = ""
  }
}

struct HashtagChip: View {
  typealias Constants = HashtagView.Constants
  let tag: String

  var body: some View {
    Text("#\(tag)")
      .font(Constants.hashtagChipFont)
      .foregroundColor(Constants.hashtagChipTextColor)
      .padding(Constants.hashtagChipPadding)
      .background(Constants.hashtagChipBackgroundColor)
      .cornerRadius(Constants.hashtagChipCornerRadius)
  }
}

extension HashtagView {
  enum Constants {
    static let vStackSpacing = 8.0
    static let addHashtagText = "Add Hashtag"
    static let addHashtagColor =  Color(red: 0.83, green: 0.83, blue: 0.83)
    static let addHashtagFont = Font.custom("Kumbh Sans", size: 14)
      .weight(.medium)
    static let enterColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let enterFont = Font.custom("Kumbh Sans", size: 14)
    static let placeholderText = "Hit enter to add #hashtags"
    static let placeholderColor =  Color(red: 0.32, green: 0.32, blue: 0.32)
    static let chipSpacing = 6.0
    static let hStackPadding = 8.0
    static let hStackBGColor = Color(red: 0.09, green: 0.09, blue: 0.09)
    static let hStackRadius = 8.0
    static let strokeColor =  Color(red: 0.64, green: 0.64, blue: 0.64)
    static let hashtagChipTextColor =  Color(red: 0.98, green: 0.98, blue: 0.98)
    static let hashtagChipFont = Font.custom("Kumbh Sans", size: 12)
    static let hashtagChipPadding: CGFloat = 5
    static let hashtagChipBackgroundColor =  Color(red: 0.25, green: 0.25, blue: 0.25)
    static let hashtagChipCornerRadius: CGFloat = 8.0
  }
}
