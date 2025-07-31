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
    let (topThreePrincipals, topThreeWins) = topThreePositions(for: response)

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

              VStack(spacing: .zero) {
                Text(topThreeWins[index].description)
                  .font(Constants.satsFont)
                  .foregroundColor(Constants.satsColour)
                  .overlay(
                    textGradientFor(index, radius: Constants.winsEndRadius)
                      .mask(
                        Text(topThreeWins[index].description)
                          .font(Constants.satsFont)
                      )
                  )

                Text(Constants.totalSats)
                  .font(Constants.gamesWonFont)
                  .foregroundColor(Constants.gamesWonColour)
                  .overlay(
                    textGradientFor(index, radius: Constants.gameWinsEndRadius)
                      .mask(
                        Text(Constants.totalSats)
                          .font(Constants.gamesWonFont)
                      )
                  )
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
                 animationSpeed: .one) {}
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
          .frame(width: rowWidth * Constants.totalSatsFactor, alignment: .trailing)
      }

      LeaderboardRowView(
        leaderboardRow: response.userRow,
        isCurrentUser: true,
        rowWidth: rowWidth,
        imageURL: viewModel.fetchImageURL(for: response.userRow.principalID)
      )

      ForEach(response.topRows, id: \.id) { leaderboardRow in
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

  private func textGradientFor(_ index: Int, radius: CGFloat) -> RadialGradient {
    if index == .one {
      Constants.goldGradient(endRadius: radius)
    } else if index == .zero {
      Constants.silverGradient(endRadius: radius)
    } else {
      Constants.bronzeGradient(endRadius: radius)
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
    static let gamesWonFont = YralFont.pt14.medium.swiftUIFont
    static let gamesWonColour = YralColor.grey50.swiftUIColor

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
    static let totalSats = "Games Won"
    static let totalSatsFont = YralFont.pt12.medium.swiftUIFont
    static let totalSatsColour = YralColor.grey600.swiftUIColor
    static let totalSatsFactor = 0.38
    static let totalSatsLeading = 48.0

    static let leaderboardVertical = 22.0
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
  }
}
