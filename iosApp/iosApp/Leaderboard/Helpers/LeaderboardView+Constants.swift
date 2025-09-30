//
//  LeaderboardView+Constants.swift
//  iosApp
//
//  Created by Samarth Paboowal on 12/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import SwiftUI

extension LeaderboardView {
  enum Constants {
    static let screenWidth = UIScreen.main.bounds.width
    static let screenHeight = UIScreen.main.bounds.height
    static let rowWidth = UIScreen.main.bounds.width - 32

    static let emptyVstackSpacing = 32.0
    static let emptyVstackWidth = 270.0
    static let emptyVstackTop = 80.0
    static let emptyTitle = "Play games to claim your spot on the leaderboard"
    static let emptyFont = YralFont.pt18.medium.swiftUIFont
    static let emptyColor = YralColor.grey0.swiftUIColor
    static let emptyCTA = "Start Playing"
    static let emptyCTAFont = YralFont.pt16.bold.swiftUIFont
    static let emptyCTAColor = Color(hex: "E2017B")
    static let emptyCTAHeight = 45.0
    static let emptyCTACornerRadius = 8.0
    static let emptyCTABackground = YralColor.grey50.swiftUIColor

    static let rewardBadgeSpacing = 4.0
    static let rewardBadgeFont = YralFont.pt12.semiBold.swiftUIFont
    static let rewardBadgeColor = YralColor.grey950.swiftUIColor
    static let rewardBadgeLeading = 8.0
    static let rewardBadgeTrailing = 4.0
    static let rewardBadgeHeight = 24.0
    static let rewardBadgeCorner = 12.0
    static let rewardBadgeBackground = YralColor.yellow100.swiftUIColor

    static let goldBadgePosition = CGPoint(x: 120.5, y: 83)
    static let silverBadgePosition = CGPoint(x: 23.5, y: 110)
    static let bronzeBadgePosition = CGPoint(x: 218.5, y: 124)

    static let background = YralColor.grey950.swiftUIColor
    static let defaultRewardCurrency = "YRAL"
    static let loader = "Yral_Loader"
    static let loaderSize = 24.0
    static let loaderLeading = 44.0
    static let loaderTrailing = 42.0

    static let header = "Leaderboard"
    static let headerFont = YralFont.pt20.bold.swiftUIFont
    static let headerColour = YralColor.grey50.swiftUIColor
    static let headerTop = 20.0
    static let headerLeading = 16.0
    static let headerBottom = 8.0
    static let dailyHeaderBackground = "leaderboard_daily_background"
    static let dailyHeaderLottie = "leaderboard_daily"
    static let alltimeHeaderBackground = "leaderboard_all_time_background"
    static let alltimeHeaderLottie = "leaderboard_all_time"

    static let headerGradientBottom = 12.0
    static let headerGradientAnimationTime = 0.25
    static let headerGradientDaily = LinearGradient(
      gradient: Gradient(stops: [
        .init(color: Color(hex: "FFC842").opacity(0.0), location: 0.0),
        .init(color: Color(hex: "F6B517"), location: 0.8967)
      ]),
      startPoint: .top,
      endPoint: .bottom
    )
    static let headerGradientAllTime = LinearGradient(
      gradient: Gradient(stops: [
        .init(color: Color(hex: "706EBB").opacity(0.0), location: 0.0633),
        .init(color: Color(hex: "7C79EA"), location: 0.8517)
      ]),
      startPoint: .top,
      endPoint: .bottom
    )

    static let tabSpacing = 8.0
    static let tabCornerRadius = 18.0
    static let tabVertical = 4.0
    static let tabHorizontal = 4.0
    static let tabFont = YralFont.pt20.bold.swiftUIFont
    static let tabActiveColor = YralColor.grey950.swiftUIColor
    static let tabInactiveColor = YralColor.grey600.swiftUIColor
    static let tabActiveBackground = YralColor.grey0.swiftUIColor
    static let tabInactiveBackground = Color.clear
    static let dailyTab = "Daily Wins"
    static let alltimeTab = "All Wins"
    static let tabHeight = 44.0
    static let tabBackground = YralColor.grey950.swiftUIColor
    static let tabExternalCornerRadius = 22.0
    static let tabExternalHorizontal = 16.0
    static let tabExternalVertical = 20.0
    static let bottomAdjustmentYralTabBar = 18.0

    static let timerInterval: TimeInterval = 0.5
    static let timerIntervalMS = 500
    static let timerAnimationInterval: TimeInterval = 0.3
    static let timerAnimationTime = 7200000
    static let timerShadowClear = Color.clear
    static let timerShadowPink = Color(hex: "E2017B")
    static let timerShadowWhite = Color.white
    static let timerTextColorPink = Color(hex: "E2017B")
    static let timerTextColorWhite = YralColor.grey50.swiftUIColor
    static let timerImageWhite = Image("leaderboard_clock")
    static let timerImagePink = Image("leaderboard_clock_pink")
    static let timerHstackSpacing = 4.0
    static let timerHstackVertical = 4.0
    static let timerHstackLeading = 4.0
    static let timerHstackTrailing = 12.0
    static let timerHstackCornerRadius = 13.0
    static let timerHstackHeight = 44.0
    static let timerImageSize = 18.0
    static let timerTextFont = YralFont.pt12.bold.swiftUIFont
    static let timerBackground = YralColor.grey950.swiftUIColor.opacity(0.8)
    static let timerShadowRadius = 20.3
    static let timerOffset = 36.0

    static let historyImage = "leaderboard_history"
    static let historyImageSize = 44.0
    static let historyImageTrailing = 28.0

    static let podiumImage = "podium"
    static let podiumSize = CGSize(width: 241, height: 146)
    static let podiumTop = 32.0
    static let podiumBottom = 20.0
    static let gamesWonSpacing = 8.0
    static let gamesWonWidth = 93.0
    static let gamesWonMinHeight = 27.0
    static let gamesWonMaxHeight = 40.0
    static let gamesWonBottom = 28.0

    static let headerTopHStackTop = 28.0
    static let headerBottomHStackSpacing = 16.0
    static let headerIDFont = YralFont.pt14.medium.swiftUIFont
    static let headerIDColour = YralColor.grey600.swiftUIColor
    static let headerIDColourWhite = YralColor.grey50.swiftUIColor

    static let satsImage = "sats"
    static let satsImageSize = 16.0
    static let satsFont = YralFont.pt14.bold.swiftUIFont
    static let satsColour = YralColor.grey600.swiftUIColor
    static let satsColourWhite = YralColor.grey50.swiftUIColor
    static let gamesWonFont = YralFont.pt12.medium.swiftUIFont
    static let gamesWonColour = YralColor.grey600.swiftUIColor
    static let gamesWonColourWhite = YralColor.grey50.swiftUIColor

    static let headerBottomHStackTop = 20.0
    static let headerBottomHStackBottom = 40.0
    static let headerBottomHStackWidth = 311.0
    static let headerBottomHStackHeight = 110.0
    static let headerBottomHStackSpacerMinLenght = 8.0

    static let dailyTopInset = 140.0
    static let alltimeTopInset = 96.0
    static let dragViewBackground = YralColor.grey950.swiftUIColor

    static let vStackSpacing = 12.0
    static let position = "Position"
    static let positionFont = YralFont.pt12.regular.swiftUIFont
    static let positionColour = YralColor.grey400.swiftUIColor
    static let positionFactor = 0.28
    static let positionFactorWithReward = 0.18
    static let id = "Player"
    static let idFont = YralFont.pt12.regular.swiftUIFont
    static let idColour = YralColor.grey400.swiftUIColor
    static let idFactor = 0.44
    static let idFactorWithReward = 0.35
    static let rewardYral = "yral_token_wallet"
    static let rewardBTC = "btc_token_wallet"
    static let reward = "Rewards"
    static let rewardFont = YralFont.pt12.regular.swiftUIFont
    static let rewardColour = YralColor.grey400.swiftUIColor
    static let rewardFactor = 0.22
    static let rewardImageSize = 14.0
    static let totalSats = "Games Won"
    static let totalSatsFont = YralFont.pt12.regular.swiftUIFont
    static let totalSatsColour = YralColor.grey400.swiftUIColor
    static let totalSatsFactor = 0.28
    static let totalSatsFactorWithReward = 0.25
    static let totalSatsLeading = 48.0

    static let leaderboardHeaderTop = 20.0
    static let leaderboardHeaderBottom = 10.0

    static let topThree = [1, 2, 3]

    static let leaderboardVertical = 20.0
    static let leaderboardHorizontal = 16.0

    static let winsEndRadius = 30.0
    static let gameWinsEndRadius = 80.0

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

    static let confetti = "leaderboard_confetti"
    static let confettiPositions: [CGPoint] = [
      CGPoint(x: 0.25, y: 0.1),
      CGPoint(x: 0.75, y: 0.27),
      CGPoint(x: 0.25, y: 0.44),
      CGPoint(x: 0.75, y: 0.61),
      CGPoint(x: 0.25, y: 0.8)
    ]
  }
}
