//
//  LeaderboardRowView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 02/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct LeaderboardRowView: View {
  let leaderboardRow: LeaderboardRowResponse
  let isCurrentUser: Bool
  let userPosition: Int
  let rowWidth: CGFloat
  let imageURL: URL?

  var body: some View {
    HStack(spacing: .zero) {
      Text("#\(userPosition)")
        .font(Constants.userPositionFont)
        .foregroundColor(foregroundColorForPosition())
        .overlay(
          buildTextGradient(
            text: "#\(userPosition)",
            font: Constants.userPositionFont,
            endRadius: Constants.positionEndRadius
          )
        )
        .padding(.horizontal, Constants.userPositionHorizontalPadding)
        .frame(width: rowWidth * Constants.userPositionWidthFactor, alignment: .leading)

      HStack(spacing: Constants.hStackSpacing) {
        buildUserImage()

        Text(leaderboardRow.principalID)
          .font(Constants.userIDFont)
          .foregroundColor(foregroundColorForPrincipal())
          .lineLimit(.one)
          .overlay(
            buildTextGradient(
              text: leaderboardRow.principalID,
              font: Constants.userIDFont,
              endRadius: Constants.principalEndRadius
            )
          )
      }
      .frame(width: rowWidth * Constants.hStackWidthFactor, alignment: .leading)

      HStack(spacing: Constants.hStackSpacing / .two) {
        Image(Constants.satsImage)
          .resizable()
          .frame(width: Constants.satsImageSize, height: Constants.satsImageSize)

        Text(leaderboardRow.coins.description)
          .font(Constants.coinsFont)
          .foregroundColor(Constants.coinsColour)
          .lineLimit(.one)
      }
      .padding(.trailing, Constants.bottomHStackTrailing)
      .padding(.leading, Constants.bottomHStackLeading)
      .frame(width: rowWidth * Constants.bottomHStackWidthFactor, alignment: .leading)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .frame(height: Constants.rowHeight)
    .background(
      buildRowBackground()
    )
    .background(isCurrentUser ? Constants.currentUserRowBackground : Constants.otherUserRowbackground)
    .overlay(
      RoundedRectangle(cornerRadius: Constants.rowCornerRadius)
        .stroke(isCurrentUser ? Constants.currentUserBorder : .clear, lineWidth: .one)
    )
    .clipShape(RoundedRectangle(cornerRadius: Constants.rowCornerRadius))
  }

  @ViewBuilder private func buildUserImage() -> some View {
    if !isCurrentUser && (userPosition == .one || userPosition == .two || userPosition == .three) {
      ZStack {
        gradientImageFor(userPosition)
          .resizable()
          .frame(width: Constants.imageSize, height: Constants.imageSize)

        URLImage(url: imageURL)
          .frame(width: Constants.borderedImageSize, height: Constants.borderedImageSize)
          .clipShape(Circle())
      }
    } else {
      URLImage(url: imageURL)
        .frame(width: Constants.imageSize, height: Constants.imageSize)
        .clipShape(Circle())
    }
  }

  @ViewBuilder private func buildRowBackground() -> some View {
    if !isCurrentUser && (userPosition == .one || userPosition == .two || userPosition == .three) {
      rectangularGradientFor(userPosition)
        .clipShape(RoundedRectangle(cornerRadius: Constants.rowCornerRadius))
    } else {
      EmptyView()
    }
  }

  @ViewBuilder private func buildTextGradient(text: String, font: Font, endRadius: CGFloat) -> some View {
    if !isCurrentUser && (userPosition == .one || userPosition == .two || userPosition == .three) {
      textGradientFor(userPosition, radius: endRadius)
      .mask(
        Text(text)
          .font(font)
      )
    } else {
      EmptyView()
    }
  }

  private func gradientImageFor(_ position: Int) -> Image {
    if position == .one {
      Image(Constants.goldRadialCircular)
    } else if position == .two {
      Image(Constants.silverRadialCircular)
    } else {
      Image(Constants.bronzeRadialCircular)
    }
  }

  private func rectangularGradientFor(_ position: Int) -> Image {
    if position == .one {
      Image(Constants.goldRadialRectangular)
    } else if position == .two {
      Image(Constants.silverRadialRectangular)
    } else {
      Image(Constants.bronzeRadialRectangular)
    }
  }

  private func foregroundColorForPrincipal() -> Color {
    if !isCurrentUser && (userPosition == .one || userPosition == .two || userPosition == .three) {
      Color.clear
    } else {
      Constants.userIDColour
    }
  }

  private func foregroundColorForPosition() -> Color {
    if !isCurrentUser && (userPosition == .one || userPosition == .two || userPosition == .three) {
      Color.clear
    } else {
      Constants.userPositionColor
    }
  }

  private func textGradientFor(_ position: Int, radius: CGFloat) -> RadialGradient {
    if position == .one {
      Constants.goldGradient(endRadius: radius)
    } else if position == .two {
      Constants.silverGradient(endRadius: radius)
    } else {
      Constants.bronzeGradient(endRadius: radius)
    }
  }
}

extension LeaderboardRowView {
  enum Constants {
    static let userPositionColor = YralColor.grey50.swiftUIColor
    static let userPositionFont = YralFont.pt14.bold.swiftUIFont
    static let userPositionHorizontalPadding = 8.0
    static let userPositionWidthFactor = 0.17

    static let userIDFont = YralFont.pt14.medium.swiftUIFont
    static let userIDColour = YralColor.grey50.swiftUIColor

    static let coinsFont = YralFont.pt14.bold.swiftUIFont
    static let coinsColour = YralColor.grey50.swiftUIColor

    static let currentUserRowBackground = YralColor.primary400.swiftUIColor
    static let otherUserRowbackground = YralColor.grey900.swiftUIColor

    static let currentUserBorder = YralColor.primary300.swiftUIColor

    static let goldRadialCircular = "gold_radial"
    static let goldRadialRectangular = "gold_radial_rectangle"
    static let silverRadialCircular = "silver_radial"
    static let silverRadialRectangular = "silver_radial_rectangle"
    static let bronzeRadialCircular = "bronze_radial"
    static let bronzeRadialRectangular = "bronze_radial_rectangle"

    static let hStackSpacing = 8.0
    static let imageSize = 28.0
    static let borderedImageSize = 24.0
    static let hStackWidthFactor = 0.45

    static let satsImage = "sats"
    static let satsImageSize = 16.0

    static let bottomHStackTrailing = 8.0
    static let bottomHStackLeading = 48.0
    static let bottomHStackWidthFactor = 0.38

    static let rowHeight = 42.0
    static let rowCornerRadius = 8.0

    static let positionEndRadius = 20.0
    static let principalEndRadius = 120.0

    static func goldGradient(endRadius: CGFloat) -> RadialGradient {
      RadialGradient(
        stops: [
          .init(color: Color(red: 191/255, green: 118/255, blue: 11/255), location: 0.0),
          .init(color: Color(red: 255/255, green: 232/255, blue: 159/255), location: 0.507),
          .init(color: Color(red: 195/255, green: 143/255, blue: 0/255), location: 1.0)
        ],
        center: UnitPoint(x: 0.0914, y: 0.0),
        startRadius: 0,
        endRadius: endRadius
      )
    }

    static func silverGradient(endRadius: CGFloat) -> RadialGradient {
      RadialGradient(
        stops: [
          .init(color: Color(red: 47/255, green: 47/255, blue: 48/255), location: 0.0),
          .init(color: Color(red: 255/255, green: 255/255, blue: 255/255), location: 0.5),
          .init(color: Color(red: 75/255, green: 75/255, blue: 75/255), location: 1.0)
        ],
        center: UnitPoint(x: 0.8443, y: 0.0),
        startRadius: 0,
        endRadius: endRadius
      )
    }

    static func bronzeGradient(endRadius: CGFloat) -> RadialGradient {
      RadialGradient(
        stops: [
          .init(color: Color(red: 109/255, green: 76/255, blue: 53/255), location: 0.0),
          .init(color: Color(red: 219/255, green: 163/255, blue: 116/255), location: 0.5),
          .init(color: Color(red: 159/255, green: 119/255, blue: 83/255), location: 1.0)
        ],
        center: UnitPoint(x: 0.1129, y: 0.3226),
        startRadius: 0,
        endRadius: endRadius
      )
    }
  }
}
