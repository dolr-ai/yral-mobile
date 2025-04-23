//
//  AboutGame.swift
//  iosApp
//
//  Created by Samarth Paboowal on 22/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct GameRule {
  let id: String
  let title: String
  let imageURL: String
  let body: [BodyBlock]
}

enum BodyBlock {
  case text([String], [String])
  case images([String])
}

struct AboutGameView: View {
  let rules: [GameRule] = [
    GameRule(id: "1",
             title: "Pick an emoji",
             imageURL: "",
             body: [
              .text(["While watching a video, vote for one of 5 emojis:"], ["grey50"]),
              .images(["default_profile", "default_profile", "default_profile", "default_profile", "default_profile"]),
              .text(["Each vote costs 10 points"], ["grey50"])
             ]),
    GameRule(id: "2",
             title: "One Vote per Video",
             imageURL: "",
             body: [
              .text(["You can only vote once per video, so choose wisely!"], ["grey50"])
             ]),
    GameRule(id: "3",
             title: "Win or Loose",
             imageURL: "",
             body: [
              .text(
                [
                  "If your emoji gets that most votes, you ",
                  "win 30 points. ",
                  "If not, ",
                  "you lose 10 points"
                ],
                [
                  "grey50",
                  "green300",
                  "grey50",
                  "red300"
                ]
              )
             ]),
    GameRule(id: "4",
             title: "Live Points Update",
             imageURL: "",
             body: [
              .text(["Your balance updates in real time on the home screen."], ["grey50"])
             ]),
    GameRule(id: "5",
             title: "Leaderboard",
             imageURL: "",
             body: [
              .text(["Top 10 users ranked by total points (tie-breaker: most wins)."], ["grey50"])
             ]),
    GameRule(id: "6",
             title: "Refer & Earn",
             imageURL: "",
             body: [
              .text(
                [
                  "Invite friends to join the fun!\n",
                  "Get 500 points ",
                  "for every successful referral"
                ],
                [
                  "grey50",
                  "yellow200",
                  "grey50"
                ]
              )
             ])
  ]

  var body: some View {
    ScrollView {
      ForEach(rules, id: \.id) { rule in
        GameRuleView(rule: rule)
          .padding(.horizontal, Constants.ruleHorizontalPadding)
          .padding(.bottom, Constants.ruleBottomPadding)
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    .padding(.top, Constants.pageTopPadding)
    .background(Constants.backgroundColor)
  }
}

struct GameRuleView: View {
  let rule: GameRule

  var body: some View {
    VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
      HStack(spacing: Constants.hstackSpacing) {
        Color.red
          .frame(width: Constants.thumbnailSize, height: Constants.thumbnailSize)
          .clipShape(Circle())

        Text(rule.title)
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
  private func buildBodyBlocks(_ blocks: [BodyBlock]) -> some View {
    ForEach(blocks.indices, id: \.self) { index in
      switch blocks[index] {
      case .images(let images):
        HStack(spacing: Constants.imagesInterItemPadding) {
          ForEach(images, id: \.self) { image in
            Image(image)
              .resizable()
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

extension AboutGameView {
  enum Constants {
    static let ruleHorizontalPadding = 16.0
    static let ruleBottomPadding = 20.0
    static let pageTopPadding = 12.0
    static let backgroundColor = YralColor.grey950.swiftUIColor
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

#Preview {
  AboutGameView()
}
