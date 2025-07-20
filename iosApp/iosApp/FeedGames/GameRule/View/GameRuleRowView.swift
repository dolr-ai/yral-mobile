//
//  GameRuleRowView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameRuleRowView: View {
  let rule: GameRuleResponse

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      HStack(spacing: Constants.hstackSpacing) {
        if let imageURL = rule.imageURL {
          FirebaseImageView(path: imageURL)
            .frame(width: Constants.thumbnailSize, height: Constants.thumbnailSize)
            .clipShape(Circle())
        }

        if let name = rule.name {
          Text(name)
            .font(Constants.titleFont)
            .foregroundColor(Constants.titleColor)
        }
      }
      .padding(.bottom, (rule.name == nil && rule.imageURL == nil) ? .zero : Constants.hstackBottomPadding)

      buildBodyBlocks(rule.body)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(Constants.ruleBottomPadding)
    .overlay(
      RoundedRectangle(cornerRadius: Constants.ruleCornerRadius)
        .stroke(Constants.borderColor, lineWidth: .one)
    )
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
      case .text(let texts, let textColors, let bolds):
        Text(getAttributedString(texts: texts, textColors: textColors, bolds: bolds))
      }
    }
  }

  private func getAttributedString(texts: [String], textColors: [String], bolds: [Bool]) -> AttributedString {
    var attributedString = AttributedString()

    for (index, text) in texts.enumerated() {
      var attrString = AttributedString(text)
      attrString.foregroundColor = Color(hex: textColors[index])
      attrString.font = bolds[index] ? Constants.subTextBoldFont : Constants.subTextFont
      attributedString += attrString
    }

    return attributedString
  }
}

extension GameRuleRowView {
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
    static let subTextFont = YralFont.pt14.swiftUIFont
    static let subTextBoldFont = YralFont.pt14.bold.swiftUIFont
    static let borderColor = YralColor.grey700.swiftUIColor
  }
}
