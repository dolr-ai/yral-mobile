//
//  SmileyGameView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Combine

struct SmileyGameView: View {
  @State var smileyGame: SmileyGame

  @State private var selectedID: String?
  @State private var isPopped = false
  @State private var isFocused = false
  @State private var showWinnerOnly = false

  let smileyTapped: (Smiley) -> Void
  let resultAnimationSubscriber: PassthroughSubject<SmileyGameResultResponse, Never>
  let initialStateSubscriber: PassthroughSubject<SmileyGame, Never>

  var body: some View {
    HStack(spacing: Constants.zero) {
      if let result = smileyGame.result {
        resultView(for: result)
      } else {
        ForEach(smileyGame.smileys, id: \.id) { smiley in
          Image(smiley.imageName)
            .resizable()
            .frame(width: Constants.smileySize, height: Constants.smileySize)
            .opacity(
              (!isFocused || smiley.id == selectedID) ? Constants.one : Constants.zero
            )
            .scaleEffect(
              (smiley.id == selectedID && isPopped) ? Constants.smileyScale : Constants.one
            )
            .shadow(
              color: (smiley.id == selectedID && isPopped) ?
              Color.white.opacity(Constants.shadownOpacity) : Color.white.opacity(.zero),
              radius: (smiley.id == selectedID && isPopped) ? Constants.shadowRadius : Constants.zero,
              x: (smiley.id == selectedID && isPopped) ? -Constants.one : Constants.zero,
              y: Constants.zero
            )
            .rotationEffect(getRotation(for: smiley))
            .animation(.easeOut(duration: Constants.durationPointTwo), value: selectedID)
            .animation(.easeOut(duration: Constants.durationPointOne), value: isFocused)
            .animation(.easeOut(duration: Constants.durationPointThree), value: showWinnerOnly)
            .onTapGesture {
              if selectedID == nil {
                AudioPlayer.shared.play(named: Constants.smileyTapAudio)
                smileyTapped(smiley)
                selectedID = smiley.id
                startPopAnimation(for: smiley)
              }
            }
            .onReceive(resultAnimationSubscriber) { result in
              startAnimation(for: result)
            }
            .padding(.vertical, Constants.smileyVerticalPadding)

          if smiley.id != smileyGame.smileys.last?.id {
            Spacer(minLength: Constants.smileySpacer)
          }
        }
      }
    }
    .onReceive(initialStateSubscriber) { game in
      setInitialState(with: game)
    }
    .frame(height: Constants.viewHeight)
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.horizontal, Constants.viewHorizontalPadding)
    .background(
      Constants.viewBackground.opacity(Constants.viewOpacity)
    )
    .clipShape(RoundedRectangle(cornerRadius: Constants.viewHeight / Constants.two))
  }

  @ViewBuilder func resultView(for result: SmileyGameResultResponse) -> some View {
    Image(result.smiley.imageName)
      .frame(width: Constants.smileySize, height: Constants.smileySize)
      .clipShape(Circle())
      .padding(.vertical, Constants.smileyVerticalPadding)
      .padding(.trailing, Constants.smileyTrailingPadding)

    VStack(alignment: .leading, spacing: Constants.two) {
      Text(result.outcome == "WIN" ?
           "\(result.smiley.imageName.capitalized) was the most people choice." :
            "Not the most popular pick!")
      .font(YralFont.pt16.bold.swiftUIFont)
      .lineLimit(Constants.textLineLimit)
      .minimumScaleFactor(Constants.textMinScale)
      .allowsTightening(true)
      .foregroundColor(YralColor.green50.swiftUIColor)

      Text(result.outcome == "WIN" ?
           "You win \(abs(result.coinDelta)) Points!" :
            "You lost \(abs(result.coinDelta)) Points")
      .font(YralFont.pt16.bold.swiftUIFont)
      .lineLimit(Constants.textLineLimit)
      .minimumScaleFactor(Constants.textMinScale)
      .allowsTightening(true)
      .foregroundColor(result.outcome == "WIN" ? YralColor.green300.swiftUIColor : YralColor.red300.swiftUIColor)
    }
  }

  private func getRotation(for smiley: Smiley) -> Angle {
    (smiley.id == selectedID && isPopped) ? Angle(degrees: -Constants.smileyRotation) : Angle(degrees: Constants.zero)
  }

  private func setInitialState(with game: SmileyGame) {
    selectedID = nil
    isPopped = false
    isFocused = false
    showWinnerOnly = false
    smileyGame = game
  }

  private func startPopAnimation(for smiley: Smiley) {
    isPopped = true

    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.durationPointSix) {
      withAnimation(.easeOut(duration: Constants.durationPointTwo)) {
        isPopped = false
      }
    }
  }

  private func startAnimation(for result: SmileyGameResultResponse) {
    isFocused = true

    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.durationPointOne) {
      guard let id = selectedID,
            let index = smileyGame.smileys.firstIndex(where: { $0.id == id })
      else {
        return
      }

      smileyGame.smileys.move(fromOffsets: IndexSet(integer: index), toOffset: .zero)
      showWinnerOnly = true
    }

    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.durationPointFour) {
      smileyGame.result = result
    }
  }
}

extension SmileyGameView {
  enum Constants {
    static let smileySize = 56.0
    static let smileyScale = 1.17
    static let smileyVerticalPadding = 4.0
    static let smileyTrailingPadding = 8.0
    static let smileySpacer = 12.0
    static let smileyRotation = 15.53
    static let shadownOpacity = 0.25
    static let shadowRadius = 5.0

    static let zero = 0.0
    static let one = 1.0
    static let two = 2.0
    static let durationPointOne = 0.1
    static let durationPointTwo = 0.2
    static let durationPointThree = 0.3
    static let durationPointFour = 0.4
    static let durationPointSix = 0.6

    static let smileyTapAudio = "smiley_tap"

    static let textLineLimit = 1
    static let textMinScale = 0.5

    static let viewHeight = 64.0
    static let viewHorizontalPadding = 12.0
    static let viewBackground = YralColor.grey950.swiftUIColor
    static let viewOpacity = 0.4
  }
}
