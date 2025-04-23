//
//  SmileyGameView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct Smiley {
  let id: String
  let name: String
  let imageURL: String
  let votes: Int
}

enum SmileyGameResult {
  case winner(Smiley, Int)
  case looser(Smiley, Int)
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
             imageURL: "",
             votes: 0),
      Smiley(id: "heart",
             name: "Heart",
             imageURL: "",
             votes: 0),
      Smiley(id: "fire",
             name: "Fire",
             imageURL: "",
             votes: 0),
      Smiley(id: "shock",
             name: "Shock",
             imageURL: "",
             votes: 0),
      Smiley(id: "rocket",
             name: "Rocket",
             imageURL: "",
             votes: 0)
    ],
    myResult: nil
  )

  @State private var selectedID: String?
  @State private var isPopped = false
  @State private var isFocused = false
  @State private var showWinnerOnly = false

  let smileyTapped: (Smiley) -> Void

  var body: some View {
    HStack(spacing: 0) {
      if let result = smileyGame.myResult {
        Color.red
          .frame(width: 48, height: 48)
          .clipShape(Circle())
          .padding(.vertical, 8)
          .padding(.trailing, 12)

        VStack(alignment: .leading, spacing: 2) {
          switch result {
          case .winner(let smiley, let points):
            Text("\(smiley.name) was the most people choice.")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.green50.swiftUIColor)

            Text("You win \(points) Points!")
              .font(YralFont.pt16.bold.swiftUIFont)
              .foregroundColor(YralColor.green300.swiftUIColor)
          case .looser(_, let points):
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
          Color.red
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
              selectedID = smiley.id
              isPopped = true

              DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                withAnimation(.easeOut(duration: 0.2)) {
                  isPopped = false
                }
              }

              DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                isFocused = true
              }

              DispatchQueue.main.asyncAfter(deadline: .now() + 1.9) {
                guard let id = selectedID,
                      let index = smileyGame.smileys.firstIndex(where: { $0.id == id })
                else {
                  return
                }

                smileyGame.smileys.move(fromOffsets: IndexSet(integer: index), toOffset: 0)
                showWinnerOnly = true
              }

              DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
                smileyGame.myResult = .winner(smiley, 30)
              }
            }
            .padding(.vertical, 8)

          if smiley.id != smileyGame.smileys.last?.id {
            Spacer(minLength: 16)
          }
        }
      }
    }
    .frame(height: 64)
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding(.horizontal, 16)
    .background(
      YralColor.grey950.swiftUIColor.opacity(0.4)
    )
    .clipShape(RoundedRectangle(cornerRadius: 32))
  }
}

#Preview {
  SmileyGameView { _ in }
}
