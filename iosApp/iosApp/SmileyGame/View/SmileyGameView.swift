//
//  SmileyGameView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Combine

struct Smiley {
  let id: String
  let name: String
  let imageName: String
}

enum SmileyGameResult {
  case winner(Smiley, Int)
  case looser(Smiley, Int)
}

extension SmileyGameResult {
  var bottomSheetHeading: String {
    switch self {
    case .winner:
      return "Congratulations!"
    case .looser:
      return "OOPS!!!"
    }
  }

  var bottomSheetTitle: String {
    switch self {
    case .winner:
      return "Since most people voted on"
    case .looser:
      return "Since most people didn't voted on"
    }
  }

  var bottomSheetSubheading: String {
    switch self {
    case .winner(_, let points):
      return "You Won \(points) Points"
    case .looser(_, let points):
      return "You Lost \(points) Points"
    }
  }

  var lottieName: String {
    switch self {
    case .winner:
      return "Smiley_Game_Win"
    case .looser:
      return "Smiley_Game_Lose"
    }
  }

  var smiley: Smiley {
    switch self {
    case .winner(let smiley, _):
      return smiley
    case .looser(let smiley, _):
      return smiley
    }
  }
}

struct SmileyGame {
  var smileys: [Smiley]
  var myResult: SmileyGameResult?
}

struct SmileyGameView: View {
  @State private var smileyGame: SmileyGame = SmileyGame(
    smileys: [
      Smiley(id: "laugh",
             name: "Laugh",
             imageName: "laugh"),
      Smiley(id: "heart",
             name: "Heart",
             imageName: "heart"),
      Smiley(id: "fire",
             name: "Fire",
             imageName: "fire"),
      Smiley(id: "shock",
             name: "Shock",
             imageName: "shock"),
      Smiley(id: "rocket",
             name: "Rocket",
             imageName: "rocket")
    ],
    myResult: nil
  )

  @State private var selectedID: String?
  @State private var isPopped = false
  @State private var isFocused = false
  @State private var showWinnerOnly = false

  let smileyTapped: (Smiley) -> Void
  let resultAnimationSubscriber: PassthroughSubject<SmileyGameResult, Never>
  let initialStateSubscriber: PassthroughSubject<SmileyGameResult?, Never>

  var body: some View {
    HStack(spacing: 0) {
      if let result = smileyGame.myResult {
        switch result {
        case .winner(let smiley, let points):
          Image(smiley.imageName)
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            .padding(.vertical, 8)
            .padding(.trailing, 12)

          VStack(alignment: .leading, spacing: 2) {
            Text("\(smiley.name) was the most people choice.")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.green50.swiftUIColor)

            Text("You win \(points) Points!")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.green300.swiftUIColor)
          }
        case .looser(let smiley, let points):
          Image(smiley.imageName)
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            .padding(.vertical, 8)
            .padding(.trailing, 12)

          VStack(alignment: .leading, spacing: 2) {
            Text("Not the most popular pick!")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.green50.swiftUIColor)

            Text("You lost \(points) Points")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.red300.swiftUIColor)
          }
        }
      } else {
        ForEach(smileyGame.smileys, id: \.id) { smiley in
          Image(smiley.imageName)
            .resizable()
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            .opacity(
              (!isFocused || smiley.id == selectedID) ? 1 : 0
            )
            .scaleEffect(
              (smiley.id == selectedID && isPopped) ? 1.17 : 1
            )
            .animation(.easeOut(duration: 0.2), value: selectedID)
            .animation(.easeOut(duration: 0.1), value: isFocused)
            .animation(.easeOut(duration: 0.3), value: showWinnerOnly)
            .onTapGesture {
              smileyTapped(smiley)
              selectedID = smiley.id
              startPopAnimation(for: smiley)
            }
            .onReceive(resultAnimationSubscriber) { result in
              startAnimation(for: result)
            }
            .padding(.vertical, 8)

          if smiley.id != smileyGame.smileys.last?.id {
            Spacer(minLength: 16)
          }
        }
      }
    }
    .onReceive(initialStateSubscriber) { result in
      setInitialState(with: result)
    }
    .frame(height: 64)
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.horizontal, 16)
    .background(
      YralColor.grey950.swiftUIColor.opacity(0.4)
    )
    .clipShape(RoundedRectangle(cornerRadius: 32))
  }

  private func setInitialState(with result: SmileyGameResult?) {
    selectedID = nil
    isPopped = false
    isFocused = false
    showWinnerOnly = false
    smileyGame = SmileyGame(
      smileys: [
        Smiley(id: "laugh",
               name: "Laugh",
               imageName: "laugh"),
        Smiley(id: "heart",
               name: "Heart",
               imageName: "heart"),
        Smiley(id: "fire",
               name: "Fire",
               imageName: "fire"),
        Smiley(id: "shock",
               name: "Shock",
               imageName: "shock"),
        Smiley(id: "rocket",
               name: "Rocket",
               imageName: "rocket")
      ],
      myResult: nil
    )
  }

  private func startPopAnimation(for smiley: Smiley) {
    isPopped = true

    DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
      withAnimation(.easeOut(duration: 0.2)) {
        isPopped = false
      }
    }
  }

  private func startAnimation(for result: SmileyGameResult) {
    isFocused = true

    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
      guard let id = selectedID,
            let index = smileyGame.smileys.firstIndex(where: { $0.id == id })
      else {
        return
      }

      smileyGame.smileys.move(fromOffsets: IndexSet(integer: index), toOffset: 0)
      showWinnerOnly = true
    }

    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
      smileyGame.myResult = result
    }
  }
}
