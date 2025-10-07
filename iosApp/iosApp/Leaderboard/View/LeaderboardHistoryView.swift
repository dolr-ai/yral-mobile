//
//  LeaderboardHistoryView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 09/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

struct LeaderboardHistoryView: View {
  @Environment(\.appDIContainer) var appDIContainer
  @ObservedObject var viewModel: LeaderboardHistoryViewModel

  @State private var showLoader = true
  @State private var showConfetti = false
  @State private var selectedDate = ""

  let onDismiss: () -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: .zero) {
      // Navigation Bar
      HStack {
        Image(Constants.backImage)
          .resizable()
          .frame(width: Constants.backImageSize, height: Constants.backImageSize)
          .onTapGesture {
            onDismiss()
          }

        Spacer()

        Text(Constants.screenTitle)
          .font(Constants.screenTitleFont)
          .foregroundColor(Constants.screenTitleColor)

        Spacer()
      }
      .padding(.bottom, Constants.navHstackBottom)
      .padding(.leading, -Constants.navHstackLeading)

      if viewModel.leaderboardHistory.count > .zero {
        // Scrollable Dates
        ScrollView(.horizontal, showsIndicators: false) {
          HStack(spacing: Constants.scrollHstackSpacing) {
            ForEach(viewModel.leaderboardHistory.indices, id: \.self) { index in
              let leaderboard = viewModel.leaderboardHistory[index]
              let leaderboardDate = leaderboard.date ?? ""

              Text(leaderboardDate)
                .font(Constants.dateFont)
                .foregroundColor(
                  leaderboardDate == selectedDate ?
                  Constants.dateSelectedColor : Constants.dateColor
                )
                .padding(.vertical, Constants.dateVertical)
                .padding(.horizontal, Constants.dateHorizontal)
                .background(
                  leaderboardDate == selectedDate ?
                  Constants.dateSelectedBackground : Constants.dateBackground
                )
                .clipShape(
                  RoundedRectangle(cornerRadius: Constants.dateCornerRadius)
                )
                .onTapGesture {
                  selectedDate = leaderboardDate
                  showConfetti = shouldShowConfetti()
                  sendDaySelectedEvent()
                }
            }
          }
          .padding(.vertical, Constants.scrollHstackVertical)
          .padding(.horizontal, Constants.scrollHstackHorizontal)
        }
        .background(Constants.scrollViewBackground)
        .padding(.horizontal, -Constants.scrollViewHorizontal)

        VStack(alignment: .leading, spacing: .zero) {
          let leaderboard = viewModel.leaderboardHistory.first(where: {
            $0.date == selectedDate
          })
          let rewardsEnabled = leaderboard?.rewardsEnabled ?? false
          let rewardsCurrency = leaderboard?.rewardCurrency ?? Constants.defaultRewardCurrency
          HStack(spacing: .zero) {
            Text(Constants.position)
              .font(Constants.positionFont)
              .foregroundColor(Constants.positionColour)
              .frame(
                width: Constants.rowWidth * (
                  rewardsEnabled ? Constants.positionFactorWithReward : Constants.positionFactor
                ),
                alignment: .leading
              )

            Text(Constants.id)
              .font(Constants.idFont)
              .foregroundColor(Constants.idColour)
              .frame(
                width: Constants.rowWidth * (
                  rewardsEnabled ? Constants.idFactorWithReward : Constants.idFactor
                ),
                alignment: .leading
              )

            if rewardsEnabled {
              HStack(spacing: .four) {
                Spacer(minLength: .zero)

                Text(Constants.reward)
                  .font(Constants.rewardFont)
                  .foregroundColor(Constants.rewardColour)
              }
              .frame(
                width: Constants.rowWidth * Constants.rewardFactor,
                alignment: .trailing
              )
            }

            Text(Constants.totalSats)
              .font(Constants.totalSatsFont)
              .foregroundColor(Constants.totalSatsColour)
              .frame(
                width: Constants.rowWidth * (
                  rewardsEnabled ? Constants.totalSatsFactorWithReward : Constants.totalSatsFactor
                ),
                alignment: .trailing
              )
          }
          .padding(.bottom, Constants.hstackBottom)

          ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: Constants.scrollVstackSpacing) {
              if let leaderboard = viewModel.leaderboardHistory.first(where: { $0.date == selectedDate }) {
                let rewardsEnabled = leaderboard.rewardsEnabled
                let rewardCurrency = leaderboard.rewardCurrency ?? Constants.defaultRewardCurrency
                let rewardCurrencyCode = leaderboard.rewardCurrencyCode
                if let userRow = leaderboard.userRow {
                  LeaderboardRowView(
                    leaderboardRow: userRow,
                    isCurrentUser: true,
                    rowWidth: Constants.rowWidth,
                    imageURL: viewModel.fetchImageURL(for: userRow.principalID),
                    rewardsEnabled: rewardsEnabled,
                    rewardCurrency: rewardCurrency,
                    rewardCurrencyCode: rewardCurrencyCode
                  )
                }

                ForEach(leaderboard.topRows, id: \.id) { leaderboardRow in
                  LeaderboardRowView(
                    leaderboardRow: leaderboardRow,
                    isCurrentUser: false,
                    rowWidth: Constants.rowWidth,
                    imageURL: viewModel.fetchImageURL(for: leaderboardRow.principalID),
                    rewardsEnabled: rewardsEnabled,
                    rewardCurrency: rewardCurrency,
                    rewardCurrencyCode: rewardCurrencyCode
                  )
                }
              }
            }
            .padding(.bottom, Constants.scrollViewVstackBottom)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .top)
          }
        }
        .padding(.top, Constants.vstackTop)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(maxHeight: .infinity, alignment: .top)
      }
    }
    .padding(.horizontal, Constants.screenPadding)
    .padding(.top, Constants.screenPadding)
    .frame(maxWidth: .infinity, alignment: .leading)
    .frame(maxHeight: .infinity, alignment: .top)
    .background(Constants.screenBackground)
    .hapticFeedback(.impact(weight: HapticFeedback.Weight.light), trigger: selectedDate)
    .overlay(alignment: .center) {
      if showLoader {
        LottieLoaderView(animationName: Constants.loader, resetProgess: false)
          .frame(width: Constants.loaderSize, height: Constants.loaderSize)
      }
    }
    .overlay(alignment: .center) {
      if showConfetti {
        ForEach(Constants.confettiPositions.indices, id: \.self) { index in
          LottieView(
            name: Constants.confetti,
            loopMode: .playOnce,
            animationSpeed: .one,
            resetProgress: false) {}
            .frame(width: Constants.screenWidth, height: Constants.screenWidth)
            .position(
              x: Constants.screenWidth * Constants.confettiPositions[index].x,
              y: Constants.screenHeight * Constants.confettiPositions[index].y
            )
            .allowsHitTesting(false)
            .id(selectedDate)
        }
      }
    }
    .onReceive(viewModel.$state) { state in
      switch state {
      case .loading:
        showLoader = true
        showConfetti = false
      case .success(let defaultDate):
        selectedDate = defaultDate
        showLoader = false
        showConfetti = shouldShowConfetti()
        sendDaySelectedEvent()
      default:
        showLoader = false
        showConfetti = false
      }
    }
    .task {
      await viewModel.fetchLeaderboardHistory()
    }
  }

  private func shouldShowConfetti() -> Bool {
    guard selectedDate != "" else {
      return false
    }

    if let leaderboard = viewModel.leaderboardHistory.first(where: { $0.date == selectedDate }) {
      return leaderboard.topRows.contains(where: {
        ($0.principalID == appDIContainer?.authClient.userPrincipalString) && (Constants.topThree.contains($0.position))
      })
    }

    return false
  }

  private func sendDaySelectedEvent() {
    guard let selectedIndex = viewModel.leaderboardHistory.firstIndex(where: {
      $0.date == selectedDate
    }) else {
      return
    }

    let leaderboard = viewModel.leaderboardHistory[selectedIndex]
    let date = leaderboard.date ?? ""
    let userRank = leaderboard.userRow?.position ?? -1

    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: LeaderBoardDaySelectedEventData(
        day: Int32(selectedIndex),
        date: date,
        rank: Int32(userRank),
        visibleRows: nil
      )
    )
  }
}

extension LeaderboardHistoryView {
  enum Constants {
    static let screenWidth = UIScreen.main.bounds.width
    static let screenHeight = UIScreen.main.bounds.height
    static let rowWidth = UIScreen.main.bounds.width - 32
    static let defaultRewardCurrency = "YRAL"
    static let loader = "Yral_Loader"
    static let loaderSize = 24.0
    static let topThree = [1, 2, 3]

    static let screenPadding = 16.0
    static let screenBackground = Color.black

    static let navHstackLeading = 4.0
    static let navHstackBottom = 32.0
    static let backImage = "chevron-left"
    static let backImageSize = 24.0
    static let screenTitle = "Weekly Wins"
    static let screenTitleFont = YralFont.pt20.bold.swiftUIFont
    static let screenTitleColor = YralColor.grey0.swiftUIColor

    static let scrollViewHorizontal = 16.0
    static let scrollViewBackground = YralColor.grey800.swiftUIColor
    static let scrollHstackSpacing = 8.0
    static let scrollHstackVertical = 4.0
    static let scrollHstackHorizontal = 16.0
    static let dateFont = YralFont.pt16.bold.swiftUIFont
    static let dateColor = YralColor.yellow300.swiftUIColor
    static let dateSelectedColor = YralColor.yellow400.swiftUIColor
    static let dateVertical = 4.0
    static let dateHorizontal = 12.0
    static let dateBackground = Color.clear
    static let dateSelectedBackground = YralColor.grey50.swiftUIColor
    static let dateCornerRadius = 16.0

    static let vstackTop = 40.0
    static let hstackBottom = 12.0
    static let position = "Rank"
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
    static let totalSats = "Wins"
    static let totalSatsFont = YralFont.pt12.regular.swiftUIFont
    static let totalSatsColour = YralColor.grey400.swiftUIColor
    static let totalSatsFactor = 0.28
    static let totalSatsFactorWithReward = 0.25
    static let scrollVstackSpacing = 12.0
    static let scrollViewVstackBottom = 40.0

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
