//
//  LeaderboardView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

// swiftlint: disable file_length
// swiftlint: disable type_body_length
struct LeaderboardView: View {
  @EnvironmentObject var session: SessionManager
  @EnvironmentObject var eventBus: EventBus
  @Environment(\.appDIContainer) private var appDIContainer
  @Environment(\.leaderboardNavController) private var navController

  @StateObject var viewModel: LeaderboardViewModel

  @State private var leaderboardRowsExpanded = false
  @State private var showLoader = true
  @State private var showConfetti = false
  @State private var mode: LeaderboardMode = .daily
  @State private var timerShadowColor = Constants.timerShadowClear
  @State private var timerTextColor = Constants.timerTextColorPink
  @State private var timerImage = Constants.timerImagePink
  @State private var timerText = ""
  @State private var headerHeight: CGFloat = .zero

  let timer = Timer.publish(every: Constants.timerInterval, on: .main, in: .common).autoconnect()

  var body: some View {
    GeometryReader { geo in
      let usableHeight = geo.size.height
      - geo.safeAreaInsets.top
      - geo.safeAreaInsets.bottom
      - Constants.bottomAdjustmentYralTabBar

      ZStack {
        VStack(alignment: .center, spacing: .zero) {
          VStack(alignment: .center, spacing: .zero) {
            buildSegmentedControl()

            if viewModel.leaderboardResponse?.timeLeftInMs != nil {
              buildTimerSection()
            }
          }
          .padding(.bottom, Constants.headerGradientBottom)
          .background(
            mode == .daily ?
            Constants.headerGradientDaily
              .opacity(leaderboardRowsExpanded ? .one : .zero)
              .animation(
                .easeOut(
                  duration: Constants.headerGradientAnimationTime
                ),
                value: leaderboardRowsExpanded
              ) :
              Constants.headerGradientAllTime
              .opacity(leaderboardRowsExpanded ? .one : .zero)
              .animation(
                .easeOut(
                  duration: Constants.headerGradientAnimationTime
                ),
                value: leaderboardRowsExpanded
              )
          )

          buildPodiumSection()
        }
        .frame(maxWidth: .infinity)
        .background(
          LottieView(
            name: mode == .daily ? Constants.dailyHeaderLottie : Constants.alltimeHeaderLottie,
            loopMode: .playOnce,
            animationSpeed: .one,
            resetProgress: false) {}
            .id(mode)
        )
        .background(
          Image(mode == .daily ? Constants.dailyHeaderBackground : Constants.alltimeHeaderBackground)
            .resizable()
            .readSize({ newSize in
              headerHeight = newSize.height
            })
        )
        .frame(maxHeight: .infinity, alignment: .top)

        if let response = viewModel.leaderboardResponse {
          DraggableView(
            isExpanded: $leaderboardRowsExpanded,
            topInset: mode == .daily ? Constants.dailyTopInset : Constants.alltimeTopInset,
            peekHeight: usableHeight - headerHeight,
            background: Constants.dragViewBackground,
            isDragEnabled: response.topRows.count > .three
          ) {
            if response.topRows.isEmpty {
              VStack(spacing: Constants.emptyVstackSpacing) {
                Text(Constants.emptyTitle)
                  .font(Constants.emptyFont)
                  .foregroundColor(Constants.emptyColor)
                  .multilineTextAlignment(.center)
                  .frame(maxWidth: .infinity)
                  .fixedSize(horizontal: false, vertical: true)

                Text(Constants.emptyCTA)
                  .font(Constants.emptyCTAFont)
                  .foregroundColor(Constants.emptyCTAColor)
                  .frame(height: Constants.emptyCTAHeight)
                  .frame(maxWidth: .infinity)
                  .background(Constants.emptyCTABackground)
                  .clipShape(
                    RoundedRectangle(cornerRadius: Constants.emptyCTACornerRadius)
                  )
                  .onTapGesture {
                    eventBus.startPlayingTapped.send(())
                  }
              }
              .frame(width: Constants.emptyVstackWidth)
              .padding(.top, Constants.emptyVstackTop)
            } else {
              buildLeaderboardHeader(response)
            }
          } content: {
            buildLeaderboard(response)
          }
          .id(mode)
          .opacity(showLoader ? .zero : .one)
        }
      }
      .frame(maxWidth: .infinity)
      .frame(maxHeight: .infinity)
      .background(Constants.background).ignoresSafeArea()
      .hapticFeedback(.impact(weight: HapticFeedback.Weight.light), trigger: mode)
      .ignoresSafeArea(.container, edges: .bottom)
      .padding(.bottom, Constants.bottomAdjustmentYralTabBar)

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
              .id(mode)
          }
        }
      }
      .onReceive(viewModel.$state, perform: { state in
        leaderboardRowsExpanded = false
        switch state {
        case .loading:
          showLoader = true
          showConfetti = false
        case .success:
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: LeaderBoardPageLoadedEventData(
              leaderBoardTabType: mode == .daily ? .daily : .all,
              rank: Int32(viewModel.leaderboardResponse?.userRow?.position ?? -1),
              visibleRows: nil
            )
          )
          showLoader = false
          showConfetti = shouldShowConfetti()
          timerText = formatMillisecondsToHMS(
            viewModel.leaderboardResponse?.timeLeftInMs ?? .zero
          )
        default:
          showLoader = false
          showConfetti = false
        }
      })
      .onReceive(timer, perform: { _ in
        if mode == .daily {
          if let timeLeftInMs = viewModel.leaderboardResponse?.timeLeftInMs, timeLeftInMs > .zero {
            if timeLeftInMs < Constants.timerAnimationTime {
              withAnimation(.easeIn(duration: Constants.timerAnimationInterval)) {
                if timerShadowColor == Constants.timerShadowClear || timerShadowColor == Constants.timerShadowWhite {
                  timerTextColor = Constants.timerTextColorWhite
                  timerImage = Constants.timerImageWhite
                  timerShadowColor = Constants.timerShadowPink
                } else {
                  timerTextColor = Constants.timerTextColorPink
                  timerImage = Constants.timerImagePink
                  timerShadowColor = Constants.timerShadowWhite
                }
              }
            }

            let newTimeLeftInMs = timeLeftInMs - Constants.timerIntervalMS
            viewModel.leaderboardResponse?.timeLeftInMs = newTimeLeftInMs
            timerText = formatMillisecondsToHMS(newTimeLeftInMs)
          }
        }
      })
      .onAppear {
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: LeaderBoardPageViewedEventData(
            leaderBoardTabType: mode == .daily ? .daily : .all
          )
        )
        showLoader = true
        Task {
          viewModel.refreshLeaderboardIfReady(for: mode)
        }
      }
    }
  }

  // swiftlint: disable function_body_length
  @ViewBuilder
  private func buildSegmentedControl() -> some View {
    HStack(spacing: Constants.tabSpacing) {
      Text(Constants.dailyTab)
        .font(Constants.tabFont)
        .foregroundColor(mode == .daily ? Constants.tabActiveColor : Constants.tabInactiveColor)
        .frame(maxWidth: .infinity)
        .frame(maxHeight: .infinity)
        .background(
          RoundedRectangle(cornerRadius: Constants.tabCornerRadius)
            .fill(mode == .daily ? Constants.tabActiveBackground : Constants.tabInactiveBackground)
        )
        .padding(.vertical, Constants.tabVertical)
        .padding(.horizontal, Constants.tabHorizontal)
        .onTapGesture {
          leaderboardRowsExpanded = false
          mode = .daily
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: LeaderBoardTabClickedEventData(
              leaderBoardTabType: .daily
            )
          )
          Task {
            viewModel.refreshLeaderboardIfReady(for: mode)
          }
        }

      Text(Constants.alltimeTab)
        .font(Constants.tabFont)
        .foregroundColor(mode == .allTime ? Constants.tabActiveColor : Constants.tabInactiveColor)
        .frame(maxWidth: .infinity)
        .frame(maxHeight: .infinity)
        .background(
          RoundedRectangle(cornerRadius: Constants.tabCornerRadius)
            .fill(mode == .allTime ? Constants.tabActiveBackground : Constants.tabInactiveBackground)
        )
        .padding(.vertical, Constants.tabVertical)
        .padding(.horizontal, Constants.tabHorizontal)
        .onTapGesture {
          leaderboardRowsExpanded = false
          mode = .allTime
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: LeaderBoardTabClickedEventData(
              leaderBoardTabType: .all
            )
          )
          Task {
            viewModel.refreshLeaderboardIfReady(for: mode)
          }
        }
    }
    .frame(maxWidth: .infinity)
    .frame(height: Constants.tabHeight)
    .background(Constants.tabBackground)
    .cornerRadius(Constants.tabExternalCornerRadius)
    .padding(.horizontal, Constants.tabExternalHorizontal)
    .padding(.vertical, Constants.tabExternalVertical)
  }
  // swiftlint: enable function_body_length

  @ViewBuilder
  private func buildTimerSection() -> some View {
    HStack(spacing: .zero) {
      Spacer(minLength: .zero)

      HStack(spacing: Constants.timerHstackSpacing) {
        timerImage
          .resizable()
          .frame(width: Constants.timerImageSize, height: Constants.timerImageSize)
          .animation(.easeIn(duration: Constants.timerAnimationInterval), value: timerImage)

        Text("Ends \(timerText)")
          .font(Constants.timerTextFont)
          .foregroundColor(timerTextColor)
          .animation(.easeIn(duration: Constants.timerAnimationInterval), value: timerTextColor)
      }
      .padding(.vertical, Constants.timerHstackVertical)
      .padding(.leading, Constants.timerHstackLeading)
      .padding(.trailing, Constants.timerHstackTrailing)
      .background(
        RoundedRectangle(cornerRadius: Constants.timerHstackCornerRadius)
          .fill(Constants.timerBackground)
      )
      .shadow(
        color: timerShadowColor,
        radius: Constants.timerShadowRadius,
        x: .zero,
        y: .zero
      )
      .offset(x: Constants.timerOffset)

      Spacer(minLength: .zero)

      Button {
        if let leaderboardHistoryDIContainer = makeLeaderboardHistoryDIContainer() {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: LeaderBoardCalendarClickedEventData(
              leaderBoardTabType: .daily,
              rank: Int32(viewModel.leaderboardResponse?.userRow?.position ?? -1)
            )
          )
          navController?.pushViewController(leaderboardHistoryDIContainer.makeLeaderboardHistoryView(onDismiss: {
            navController?.popViewController(animated: true)
          }), animated: true)
        }
      } label: {
        Image(Constants.historyImage)
          .resizable()
          .frame(width: Constants.historyImageSize, height: Constants.historyImageSize)
      }
      .padding(.trailing, Constants.historyImageTrailing)
    }
    .frame(maxWidth: .infinity)
    .frame(height: Constants.timerHstackHeight)
  }

  // swiftlint: disable function_body_length
  @ViewBuilder
  private func buildPodiumSection() -> some View {
    Image(Constants.podiumImage)
      .resizable()
      .frame(width: Constants.podiumSize.width, height: Constants.podiumSize.height)
      .padding(.top, Constants.podiumTop)
      .padding(.bottom, Constants.podiumBottom)
      .overlay(
        ZStack {
          buildRewardBadge(position: .one)
            .position(
              x: Constants.goldBadgePosition.x,
              y: Constants.goldBadgePosition.y
            )

          buildRewardBadge(position: .two)
            .position(
              x: Constants.silverBadgePosition.x,
              y: Constants.silverBadgePosition.y
            )

          buildRewardBadge(position: .three)
            .position(
              x: Constants.bronzeBadgePosition.x,
              y: Constants.bronzeBadgePosition.y
            )
        }
      )

    if let response = viewModel.leaderboardResponse {
      let topThreePrincipals = topThreePositions(for: response)
      HStack(spacing: Constants.headerBottomHStackSpacing) {
        ForEach(topThreePrincipals.indices, id: \.self) { index in
          VStack(spacing: .zero) {
            Text(condensedIDString(for: topThreePrincipals[index]))
              .font(Constants.headerIDFont)
              .foregroundColor(mode == .daily ? Constants.headerIDColour : Constants.headerIDColourWhite)
              .multilineTextAlignment(.center)
              .lineLimit(topThreePrincipals[index].count == .one ? .one : .two)
              .truncationMode(.tail)
          }
          .frame(width: Constants.gamesWonWidth)
        }
      }
      .frame(width: Constants.headerBottomHStackWidth)
      .frame(
        height: topThreePrincipals.contains { $0.count > 2 } ?
        Constants.gamesWonMaxHeight : Constants.gamesWonMinHeight
      )
      .padding(.bottom, Constants.gamesWonBottom)
    } else if showLoader {
      HStack {
        LottieLoaderView(animationName: Constants.loader, resetProgess: false)
          .frame(width: Constants.loaderSize, height: Constants.loaderSize)
          .padding(.leading, Constants.loaderLeading)

        Spacer()

        LottieLoaderView(animationName: Constants.loader, resetProgess: false)
          .frame(width: Constants.loaderSize, height: Constants.loaderSize)

        Spacer()

        LottieLoaderView(animationName: Constants.loader, resetProgess: false)
          .frame(width: Constants.loaderSize, height: Constants.loaderSize)
          .padding(.trailing, Constants.loaderTrailing)
      }
      .frame(width: Constants.headerBottomHStackWidth)
      .frame(height: Constants.gamesWonMinHeight)
      .padding(.bottom, Constants.gamesWonBottom)
    }
  }
  // swiftlint: enable function_body_length

  @ViewBuilder
  private func buildRewardBadge(position: Int) -> some View {
    if let leaderboard = viewModel.leaderboardResponse,
       let rewardsTable = leaderboard.rewardsTable,
       leaderboard.rewardsEnabled {
      let rewardCurrency = leaderboard.rewardCurrency ?? Constants.defaultRewardCurrency
      HStack(spacing: Constants.rewardBadgeSpacing) {
        Text("\(leaderboard.rewardCurrencyCode?.currencySymbol ?? "")\(rewardsTable[String(position)] ?? 0)")
          .font(Constants.rewardBadgeFont)
          .foregroundColor(Constants.rewardBadgeColor)

        Image(rewardCurrency == Constants.defaultRewardCurrency ? Constants.rewardYral : Constants.rewardBTC)
          .resizable()
          .frame(width: Constants.rewardImageSize, height: Constants.rewardImageSize)
      }
      .padding(.leading, Constants.rewardBadgeLeading)
      .padding(.trailing, Constants.rewardBadgeTrailing)
      .frame(height: Constants.rewardBadgeHeight)
      .background(Constants.rewardBadgeBackground)
      .clipShape(
        RoundedRectangle(cornerRadius: Constants.rewardBadgeCorner)
      )
    }
  }

  @ViewBuilder
  private func buildLeaderboardHeader(_ response: LeaderboardResponse) -> some View {
    let rewardsEnabled = response.rewardsEnabled

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
    .padding(.top, Constants.leaderboardHeaderTop)
    .padding(.bottom, Constants.leaderboardHeaderBottom)
  }

  @ViewBuilder
  private func buildLeaderboard(_ response: LeaderboardResponse) -> some View {
    let rewardsEnabled = response.rewardsEnabled
    let rewardCurrency = response.rewardCurrency ?? "YRAL"
    let rewardCurrencyCode = response.rewardCurrencyCode

    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      if let userRow = response.userRow {
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

      ForEach(response.topRows, id: \.id) { leaderboardRow in
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
    .padding(.bottom, Constants.leaderboardVertical)
    .padding(.horizontal, Constants.leaderboardHorizontal)
    .frame(maxWidth: .infinity, alignment: .leading)
  }

  private func topThreePositions(for response: LeaderboardResponse) -> [[String]] {

    var grouped: [Int: (ids: [String], wins: Int)] = [:]

    for row in response.topRows where (.one ... .three).contains(row.position) {
      if grouped[row.position] == nil {
        grouped[row.position] = (ids: [], wins: row.wins)
      }
      grouped[row.position]!.ids.append(row.principalID)
    }

    let order: [Int] = [.two, .one, .three]
    let idsByPosition   = order.map { grouped[$0]?.ids   ?? [] }

    return idsByPosition
  }

  private func condensedIDString(for ids: [String]) -> String {
    let firstFour = Array(ids.prefix(.four))
    guard !firstFour.isEmpty else { return "" }

    if firstFour.count == .one {
      return firstFour[.zero]
    } else {
      return firstFour
        .map { String($0.prefix(.four)) }
        .joined(separator: "..., ")
        .appending("...")
    }
  }

  private func shouldShowConfetti() -> Bool {
    if let leaderboard = viewModel.leaderboardResponse {
      return leaderboard.topRows.contains(where: {
        ($0.principalID == appDIContainer?.authClient.userPrincipalString) && (Constants.topThree.contains($0.position))
      })
    }

    return false
  }

  func formatMillisecondsToHMS(_ milliseconds: Int) -> String {
    let totalSeconds = milliseconds / 1000
    let hours = totalSeconds / 3600
    let minutes = (totalSeconds % 3600) / 60
    let seconds = totalSeconds % 60

    return String(format: "%02d : %02d : %02d", hours, minutes, seconds)
  }
}
// swiftlint: enable type_body_length

extension LeaderboardView {
  private func makeLeaderboardHistoryDIContainer() -> LeaderboardHistoryDIContainer? {
    guard let authClient = appDIContainer?.authClient else {
      return nil
    }

    let crashReporter = CompositeCrashReporter(
      reporters: [FirebaseCrashlyticsReporter()]
    )

    return LeaderboardHistoryDIContainer(
      dependencies: LeaderboardHistoryDIContainer.Dependencies(
        firebaseService: FirebaseService(),
        httpService: HTTPService(baseURLString: AppConfiguration().firebaseBaseURLString),
        crashReporter: crashReporter,
        authClient: authClient
      )
    )
  }
}
// swiftlint: enable file_length
