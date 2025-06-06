//
//  GameRuleView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameRuleView: View {
  let rule: SmileyGameRuleResponse

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      HStack(spacing: Constants.hstackSpacing) {
        FirebaseImageView(path: rule.imageURL)
          .frame(width: Constants.thumbnailSize, height: Constants.thumbnailSize)
          .clipShape(Circle())

        Text(rule.name)
          .font(Constants.titleFont)
          .foregroundColor(Constants.titleColor)
      }
      .padding(.bottom, Constants.hstackBottomPadding)

      buildBodyBlocks(rule.body)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(Constants.ruleBottomPadding)
    .background(Constants.backgroundColor)
    .clipShape(RoundedRectangle(cornerRadius: Constants.ruleCornerRadius))
  }

  @ViewBuilder
  private func buildBodyBlocks(_ blocks: [BodyElement]) -> some View {
    ForEach(blocks.indices, id: \.self) { index in
      switch blocks[index] {
      case .images(let imageURLs):
        HStack(spacing: Constants.imagesInterItemPadding) {
          ForEach(imageURLs, id: \.self) { imageURL in
            FirebaseImageView(path: imageURL)
              .frame(width: Constants.imageSize, height: Constants.imageSize)
              .clipShape(Circle())
          }
        }
        .padding(.vertical, Constants.imagesVerticalPadding)
      case .text(let texts, let textColors):
        Text(getAttributedString(texts: texts, textColors: textColors))
      }
    }
  }

  private func getAttributedString(texts: [String], textColors: [String]) -> AttributedString {
    var attributedString = AttributedString()

    for (index, text) in texts.enumerated() {
      var attrString = AttributedString(text)
      attrString.foregroundColor = YralColor(rawValue: textColors[index])?.swiftUIColor
      attributedString += attrString
    }

    return attributedString
  }
}

extension GameRuleView {
  enum Constants {
    static let vstackSpacing = 0.0
    static let hstackSpacing = 8.0
    static let hstackBottomPadding = 8.0
    static let thumbnailSize = 36.0
    static let ruleBottomPadding = 12.0
    static let ruleCornerRadius = 8.0
    static let imagesInterItemPadding = 6.0
    static let imagesVerticalPadding = 2.0
    static let imageSize = 24.0
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey50.swiftUIColor
    static let backgroundColor = YralColor.grey900.swiftUIColor
  }
}
