//
//  LeaderboardView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct LeaderboardView: View {
  @EnvironmentObject var session: SessionManager

  @StateObject var viewModel: LeaderboardViewModel

  @State private var showLeaderboard: Bool = false
  @State private var showLoader: Bool = true

  let rowWidth = UIScreen.main.bounds.width - 32

  var body: some View {
      ScrollView {
        if let leaderboard = viewModel.leaderboardResponse, showLeaderboard {
          buildHeader(leaderboard)
          buildLeaderboard(leaderboard)
        }
      }
      .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
      .background(Constants.background).ignoresSafeArea()
      .overlay(alignment: .center, content: {
        if showLoader {
          LottieLoaderView(animationName: Constants.loader)
            .frame(width: Constants.loaderSize, height: Constants.loaderSize)
        }
      })
      .onReceive(viewModel.$state, perform: { state in
        switch state {
        case .loading:
          showLoader = true
          showLeaderboard = false
        case .success:
          showLoader = false
          showLeaderboard = true
        default:
          showLoader = false
          showLeaderboard = false
        }
      })
      .onAppear {
        showLoader = true
        Task {
          await viewModel.refreshLeaderboardIfReady()
        }
      }
  }

  // swiftlint: disable function_body_length
  @ViewBuilder
  private func buildHeader(_ response: LeaderboardResponse) -> some View {
    let (topThreePrincipals, topThreeCoins) = topThreePositions(for: response)

    VStack(alignment: .leading, spacing: .zero) {
      Text(Constants.header)
        .font(Constants.headerFont)
        .foregroundColor(Constants.headerColour)
        .padding(.leading, Constants.headerLeading)
        .padding(.top, UIApplication.shared.topSafeAreaInset + Constants.headerTop)
        .padding(.bottom, Constants.headerBottom)

      HStack(spacing: .zero) {
        Spacer(minLength: .zero)

        Image(Constants.podiumImage)
          .resizable()
          .frame(width: Constants.podiumSize.width, height: Constants.podiumSize.height)

        Spacer(minLength: .zero)
      }
      .padding(.top, Constants.headerTopHStackTop)

      HStack(spacing: .zero) {
        Spacer(minLength: .zero)

        HStack(spacing: Constants.headerBottomHStackSpacing) {
          ForEach(topThreePrincipals.indices, id: \.self) { index in
            VStack(spacing: .zero) {
              Text(condensedIDString(for: topThreePrincipals[index]))
                .font(Constants.headerIDFont)
                .foregroundColor(Constants.headerIDColour)
                .multilineTextAlignment(.center)
                .lineLimit(topThreePrincipals[index].count == .one ? .one : .two)
                .truncationMode(.tail)

              Spacer(minLength: Constants.headerBottomHStackSpacerMinLenght)

              HStack(spacing: .four) {
                Image(Constants.satsImage)
                  .resizable()
                  .frame(width: Constants.satsImageSize, height: Constants.satsImageSize)

                Text(topThreeCoins[index].description)
                  .font(Constants.satsFont)
                  .foregroundColor(Constants.satsColour)
              }
            }
          }
        }
        .frame(width: Constants.headerBottomHStackWidth)
        .frame(maxHeight: Constants.headerBottomHStackHeight)

        Spacer(minLength: .zero)
      }
      .padding(.top, Constants.headerBottomHStackTop)
      .padding(.bottom, Constants.headerBottomHStackBottom)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .background(
      LottieView(name: Constants.headerLottie,
                 loopMode: .playOnce,
                 animationSpeed: .one,
                 contentMode: .scaleAspectFill) {}
        .offset(x: .three, y: .thirteen)
    )
    .background(Constants.headerBackground)
  }
  // swiftlint: enable function_body_length

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
          .padding(.leading, Constants.idLeading)
          .frame(width: rowWidth * Constants.idFactor, alignment: .leading)

        Text(Constants.totalSats)
          .font(Constants.totalSatsFont)
          .foregroundColor(Constants.totalSatsColour)
          .padding(.leading, Constants.totalSatsLeading)
          .frame(width: rowWidth * Constants.totalSatsFactor, alignment: .leading)
      }

      LeaderboardRowView(
        leaderboardRow: response.userRow,
        isCurrentUser: true,
        rowWidth: rowWidth,
        imageURL: viewModel.fetchImageURL(for: response.userRow.principalID)
      )

      ForEach(response.rows, id: \.id) { leaderboardRow in
        LeaderboardRowView(
          leaderboardRow: leaderboardRow,
          isCurrentUser: false,
          rowWidth: rowWidth,
          imageURL: viewModel.fetchImageURL(for: leaderboardRow.principalID)
        )
      }
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.vertical, Constants.leaderboardVertical)
    .padding(.horizontal, Constants.leaderboardHorizontal)
  }

  private func topThreePositions(for response: LeaderboardResponse) -> ([[String]], [Int]) {

    var grouped: [Int: (ids: [String], coins: Int)] = [:]

    for row in response.rows where (.one ... .three).contains(row.position) {
      if grouped[row.position] == nil {
        grouped[row.position] = (ids: [], coins: row.coins)
      }
      grouped[row.position]!.ids.append(row.principalID)
    }

    let order: [Int] = [.two, .one, .three]
    let idsByPosition   = order.map { grouped[$0]?.ids   ?? [] }
    let coinsByPosition = order.map { grouped[$0]?.coins ?? .zero }

    return (idsByPosition, coinsByPosition)
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
}

extension LeaderboardView {
  enum Constants {
    static let background = YralColor.grey950.swiftUIColor
    static let loader = "Yral_Loader"
    static let loaderSize = 24.0

    static let header = "Leaderboard"
    static let headerFont = YralFont.pt20.bold.swiftUIFont
    static let headerColour = YralColor.grey50.swiftUIColor
    static let headerTop = 16.0
    static let headerLeading = 16.0
    static let headerBottom = 8.0
    static let headerBackground = YralColor.yellow400.swiftUIColor
    static let headerLottie = "leaderboard"

    static let podiumImage = "podium"
    static let podiumSize = CGSize(width: 245, height: 166)
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
    static let headerIDColour = YralColor.grey400.swiftUIColor

    static let satsImage = "sats"
    static let satsImageSize = 16.0
    static let satsFont = YralFont.pt14.bold.swiftUIFont
    static let satsColour = YralColor.grey50.swiftUIColor

    static let headerBottomHStackTop = 20.0
    static let headerBottomHStackBottom = 40.0
    static let headerBottomHStackWidth = 311.0
    static let headerBottomHStackHeight = 110.0
    static let headerBottomHStackSpacerMinLenght = 8.0

    static let vStackSpacing = 12.0
    static let position = "Position"
    static let positionFont = YralFont.pt12.medium.swiftUIFont
    static let positionColour = YralColor.grey600.swiftUIColor
    static let positionFactor = 0.17
    static let id = "Player ID"
    static let idFont = YralFont.pt12.medium.swiftUIFont
    static let idColour = YralColor.grey600.swiftUIColor
    static let idFactor = 0.45
    static let idLeading = 28.0
    static let totalSats = "Player SATS"
    static let totalSatsFont = YralFont.pt12.medium.swiftUIFont
    static let totalSatsColour = YralColor.grey600.swiftUIColor
    static let totalSatsFactor = 0.38
    static let totalSatsLeading = 48.0

    static let leaderboardVertical = 22.0
    static let leaderboardHorizontal = 16.0

    static let arbNumber = 999
  }
}
