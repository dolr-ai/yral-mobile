//
//  LeaderboardView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

// swiftlint: disable file_length
// swiftlint: disable type_body_length
struct LeaderboardView: View {
  @EnvironmentObject var session: SessionManager
  @Environment(\.appDIContainer) private var appDIContainer
  @Environment(\.leaderboardNavController) private var navController

  @StateObject var viewModel: LeaderboardViewModel

  @State private var leaderboardRowsExpanded = false
  @State private var showLoader = true
  @State private var showConfetti = false
  @State private var mode: LeaderboardMode = .daily
  @State private var timerShadowColor = Color.clear
  @State private var timerTextColor = YralColor.grey50.swiftUIColor
  @State private var timerImage = Image("leaderboard_clock")
  @State private var timerText = ""

  @State private var headerHeight: CGFloat = 0

  let rowWidth = UIScreen.main.bounds.width - 32
  let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

  var body: some View {
    GeometryReader { geo in
      let usableHeight = geo.size.height - geo.safeAreaInsets.top - geo.safeAreaInsets.bottom
      ZStack {
        VStack(alignment: .center, spacing: .zero) {
          buildSegmentedControl()

          if viewModel.leaderboardResponse?.timeLeftInMs == nil {
            buildTimerSection().hidden()
          } else {
            buildTimerSection()
          }

          buildPodiumSection()
        }
        .frame(maxWidth: .infinity)
        .background(
          LottieView(
            name: mode == .daily ? "leaderboard_daily" : "leaderboard_all_time",
            loopMode: .playOnce,
            animationSpeed: .one,
            resetProgress: false) {}
            .id(mode)
        )
        .background(
          Image(mode == .daily ? "leaderboard_daily_background" : "leaderboard_all_time_background")
            .resizable()
            .readSize({ newSize in
              headerHeight = newSize.height
            })
        )
        .frame(maxHeight: .infinity, alignment: .top)
        .onTapGesture {
          leaderboardRowsExpanded = false
        }

        if let response = viewModel.leaderboardResponse {
          DraggableView(
            isExpanded: $leaderboardRowsExpanded,
            topInset: mode == .daily ? 140 : 96,
            peekHeight: usableHeight - headerHeight,
            background: YralColor.grey950.swiftUIColor
          ) {
              buildLeaderboard(response)
            }
          .id(mode)
          .opacity(showLoader ? 0 : 1)
        }
      }
      .frame(maxWidth: .infinity)
      .frame(maxHeight: .infinity)
      .background(Constants.background).ignoresSafeArea()
      .hapticFeedback(.impact(weight: .light), trigger: mode)
      .overlay(alignment: .center, content: {
        if showLoader {
          LottieLoaderView(animationName: Constants.loader, resetProgess: false)
            .frame(width: Constants.loaderSize, height: Constants.loaderSize)
        }
      })
      .overlay(alignment: .center) {
        if showConfetti {
          ForEach(Constants.confettiPositions.indices, id: \.self) { index in
            LottieView(
              name: Constants.confetti,
              loopMode: .playOnce,
              animationSpeed: .one,
              resetProgress: false) {}
              .frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.width)
              .position(
                x: UIScreen.main.bounds.width * Constants.confettiPositions[index].x,
                y: UIScreen.main.bounds.height * Constants.confettiPositions[index].y
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
          showLoader = false
          showConfetti = shouldShowConfetti()
          timerText = formatMillisecondsToHMS(
            viewModel.leaderboardResponse?.timeLeftInMs ?? 0
          )
        default:
          showLoader = false
          showConfetti = false
        }
      })
      .onReceive(timer, perform: { _ in
        if mode == .daily {
          if timerShadowColor == Color.clear || timerShadowColor == Color.white {
            timerTextColor = YralColor.grey50.swiftUIColor
            timerImage = Image("leaderboard_clock")
            timerShadowColor = Color(hex: "E2017B")
          } else {
            timerTextColor = Color(hex: "E2017B")
            timerImage = Image("leaderboard_clock_pink")
            timerShadowColor = Color.white
          }

          if let timeLeftInMs = viewModel.leaderboardResponse?.timeLeftInMs, timeLeftInMs > 0 {
            let newTimeLeftInMs = timeLeftInMs - 1000
            viewModel.leaderboardResponse?.timeLeftInMs = newTimeLeftInMs
            timerText = formatMillisecondsToHMS(newTimeLeftInMs)
          }
        }
      })
      .onAppear {
        showLoader = true
        Task {
          viewModel.refreshLeaderboardIfReady(for: mode)
        }
      }
    }
  }

  @ViewBuilder
  private func buildSegmentedControl() -> some View {
    HStack(spacing: 8) {
      Text("Daily Wins")
        .font(YralFont.pt20.bold.swiftUIFont)
        .foregroundColor(mode == .daily ? YralColor.grey950.swiftUIColor : YralColor.grey600.swiftUIColor)
        .frame(maxWidth: .infinity)
        .frame(maxHeight: .infinity)
        .background(
          RoundedRectangle(cornerRadius: 18)
            .fill(mode == .daily ? YralColor.grey0.swiftUIColor : Color.clear)
        )
        .padding(.vertical, 4)
        .padding(.horizontal, 4)
        .onTapGesture {
          leaderboardRowsExpanded = false
          mode = .daily
          Task {
            viewModel.refreshLeaderboardIfReady(for: mode)
          }
        }

      Text("All Wins")
        .font(YralFont.pt20.bold.swiftUIFont)
        .foregroundColor(mode == .allTime ? YralColor.grey950.swiftUIColor : YralColor.grey600.swiftUIColor)
        .frame(maxWidth: .infinity)
        .frame(maxHeight: .infinity)
        .background(
          RoundedRectangle(cornerRadius: 18)
            .fill(mode == .allTime ? YralColor.grey0.swiftUIColor : Color.clear)
        )
        .padding(.vertical, 4)
        .padding(.horizontal, 4)
        .onTapGesture {
          viewModel.leaderboardResponse?.timeLeftInMs = nil
          leaderboardRowsExpanded = false
          mode = .allTime
          Task {
            viewModel.refreshLeaderboardIfReady(for: mode)
          }
        }
    }
    .frame(maxWidth: .infinity)
    .frame(height: 44)
    .background(YralColor.grey950.swiftUIColor)
    .cornerRadius(22)
    .padding(.horizontal, 16)
    .padding(.top, 20)
    .padding(.bottom, 20)
  }

  @ViewBuilder
  private func buildTimerSection() -> some View {
    HStack(spacing: .zero) {
      Spacer(minLength: .zero)

      HStack(spacing: 4) {
        timerImage
          .resizable()
          .frame(width: 18, height: 18)

        Text("Ends \(timerText)")
          .font(YralFont.pt12.bold.swiftUIFont)
          .foregroundColor(timerTextColor)
      }
      .padding(.vertical, 4)
      .padding(.leading, 4)
      .padding(.trailing, 12)
      .background(
        RoundedRectangle(cornerRadius: 13)
          .fill(YralColor.grey950.swiftUIColor.opacity(0.8))
      )
      .shadow(
        color: timerShadowColor,
        radius: 20.3,
        x: 0,
        y: 0
      )
      .offset(x: 36)

      Spacer(minLength: .zero)

      Button {
        if let leaderboardHistoryDIContainer = makeLeaderboardHistoryDIContainer() {
          navController?.pushViewController(leaderboardHistoryDIContainer.makeLeaderboardHistoryView(onDismiss: {
            navController?.popViewController(animated: true)
          }), animated: true)
        }
      } label: {
        Image("leaderboard_history")
          .resizable()
          .frame(width: 44, height: 44)
      }
      .padding(.trailing, 28)
    }
    .frame(maxWidth: .infinity)
    .frame(height: 44)
  }

  @ViewBuilder
  private func buildPodiumSection() -> some View {
    Image(Constants.podiumImage)
      .resizable()
      .frame(width: Constants.podiumSize.width, height: Constants.podiumSize.height)
      .padding(.top, 44)
      .padding(.bottom, 20)

    if let response = viewModel.leaderboardResponse {
      let (topThreePrincipals, topThreeWins) = topThreePositions(for: response)
      HStack(spacing: Constants.headerBottomHStackSpacing) {
        ForEach(topThreePrincipals.indices, id: \.self) { index in
          VStack(spacing: .zero) {
            Text(condensedIDString(for: topThreePrincipals[index]))
              .font(Constants.headerIDFont)
              .foregroundColor(mode == .daily ? Constants.headerIDColour : Constants.headerIDColourWhite)
              .multilineTextAlignment(.center)
              .lineLimit(topThreePrincipals[index].count == .one ? .one : .two)
              .truncationMode(.tail)

            Spacer(minLength: 8.0)

            VStack(spacing: .zero) {
              Text(topThreeWins[index].description)
                .font(Constants.satsFont)
                .foregroundColor(mode == .daily ? Constants.satsColour : Constants.satsColourWhite)

              Text(Constants.totalSats)
                .font(Constants.gamesWonFont)
                .foregroundColor(mode == .daily ? Constants.gamesWonColour : Constants.gamesWonColourWhite)
            }
          }
          .frame(width: 93)
        }
      }
      .frame(width: Constants.headerBottomHStackWidth)
      .frame(height: topThreePrincipals.contains { $0.count > 1 } ? 80 : 67)
      .padding(.bottom, 28)
    }
    .frame(maxWidth: .infinity)
    .background(
      LottieView(
        name: mode == .daily ? "leaderboard_daily" : "leaderboard_all_time",
        loopMode: .playOnce,
        animationSpeed: .one) {}
    )
    .background(
      Image(mode == .daily ? "leaderboard_daily_background" : "leaderboard_all_time_background")
        .resizable()
    )
    .frame(maxHeight: .infinity, alignment: .top)
  }

  @ViewBuilder
  private func buildLeaderboard(_ response: LeaderboardResponse) -> some View {
    VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
      HStack(spacing: .zero) {
        Text(Constants.position)
          .font(Constants.positionFont)
          .foregroundColor(Constants.positionColour)
          .frame(width: rowWidth * Constants.positionFactor, alignment: .leading)

        Text(Constants.id)
          .font(Constants.idFont)
          .foregroundColor(Constants.idColour)
          .frame(width: rowWidth * Constants.idFactor, alignment: .leading)

        Text(Constants.totalSats)
          .font(Constants.totalSatsFont)
          .foregroundColor(Constants.totalSatsColour)
          .frame(width: rowWidth * Constants.totalSatsFactor, alignment: .trailing)
      }

      if let userRow = response.userRow {
        LeaderboardRowView(
          leaderboardRow: userRow,
          isCurrentUser: true,
          rowWidth: rowWidth,
          imageURL: viewModel.fetchImageURL(for: userRow.principalID)
        )
      }

      ForEach(response.topRows, id: \.id) { leaderboardRow in
        LeaderboardRowView(
          leaderboardRow: leaderboardRow,
          isCurrentUser: false,
          rowWidth: rowWidth,
          imageURL: viewModel.fetchImageURL(for: leaderboardRow.principalID)
        )
      }
    }
    .padding(.vertical, Constants.leaderboardVertical)
    .padding(.horizontal, Constants.leaderboardHorizontal)
    .frame(maxWidth: .infinity, alignment: .leading)
  }

  private func topThreePositions(for response: LeaderboardResponse) -> ([[String]], [Int]) {

    var grouped: [Int: (ids: [String], wins: Int)] = [:]

    for row in response.topRows where (.one ... .three).contains(row.position) {
      if grouped[row.position] == nil {
        grouped[row.position] = (ids: [], wins: row.wins)
      }
      grouped[row.position]!.ids.append(row.principalID)
    }

    let order: [Int] = [.two, .one, .three]
    let idsByPosition   = order.map { grouped[$0]?.ids   ?? [] }
    let winsByPosition = order.map { grouped[$0]?.wins ?? .zero }

    return (idsByPosition, winsByPosition)
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
        ($0.principalID == appDIContainer?.authClient.userPrincipalString) && ([1, 2, 3].contains($0.position))
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

extension LeaderboardView {
  enum Constants {
    static let background = YralColor.grey950.swiftUIColor
    static let loader = "Yral_Loader"
    static let loaderSize = 24.0

    static let header = "Leaderboard"
    static let headerFont = YralFont.pt20.bold.swiftUIFont
    static let headerColour = YralColor.grey50.swiftUIColor
    static let headerTop = 20.0
    static let headerLeading = 16.0
    static let headerBottom = 8.0
    static let headerBackground = YralColor.yellow400.swiftUIColor
    static let headerLottie = "leaderboard"

    static let podiumImage = "podium"
    static let podiumSize = CGSize(width: 241, height: 146)
    static let goldRadial = "gold_radial"
    static let goldSize = 57.0
    static let goldBorderSize = 65.0
    static let goldOffset = (x: 0.0, y: 65.0)
    static let silverRadial = "silver_radial"
    static let silverBorderSize = 50.0
    static let silverOffset = (x: 97.0, y: 25.0)
    static let bronzeRadial = "bronze_radial"
    static let bronzeSize = 43.0
    static let bronzeBorderSize = 48.0
    static let bronzeOffset = (x: 100.0, y: 10.0)

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

    static let vStackSpacing = 12.0
    static let position = "Position"
    static let positionFont = YralFont.pt12.medium.swiftUIFont
    static let positionColour = YralColor.grey600.swiftUIColor
    static let positionFactor = 0.28
    static let id = "Player ID"
    static let idFont = YralFont.pt12.medium.swiftUIFont
    static let idColour = YralColor.grey600.swiftUIColor
    static let idFactor = 0.44
    static let idLeading = 28.0
    static let totalSats = "Games Won"
    static let totalSatsFont = YralFont.pt12.medium.swiftUIFont
    static let totalSatsColour = YralColor.grey600.swiftUIColor
    static let totalSatsFactor = 0.28
    static let totalSatsLeading = 48.0

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
// swiftlint: enable file_length
