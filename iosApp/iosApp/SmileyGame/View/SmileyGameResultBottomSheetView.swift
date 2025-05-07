//
//  SmileyGameResultBottomSheetView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SmileyGameResultBottomSheetView: View {
  var gameResult: SmileyGameResult

  let onKeepPlayingTapped: () -> Void
  let onLearnMoreTapped: () -> Void

  var body: some View {
    ZStack(alignment: .bottom) {
      Color.black.opacity(0.8)
        .ignoresSafeArea()

      VStack(spacing: 0) {
        RoundedRectangle(cornerRadius: 1)
          .frame(width: 32, height: 2)
          .foregroundColor(YralColor.grey500.swiftUIColor)
          .padding(.top, 12)

        Text(gameResult.bottomSheetHeading)
          .font(YralFont.pt18.bold.swiftUIFont)
          .foregroundColor(YralColor.grey0.swiftUIColor)
          .padding(.top, 28)

        LottieView(
          name: gameResult.lottieName,
          loopMode: .playOnce,
          animationSpeed: .one
        ) {}
          .frame(width: 250)
          .frame(height: 150)

        HStack(spacing: 8) {
          Text(gameResult.bottomSheetTitle)
            .font(YralFont.pt16.medium.swiftUIFont)
            .foregroundColor(YralColor.green50.swiftUIColor)

          Image(gameResult.smiley.imageName)
            .resizable()
            .frame(width: 28, height: 28)
        }
        .padding(.top, 12)

        Text(gameResult.bottomSheetSubheading)
          .font(YralFont.pt18.bold.swiftUIFont)
          .foregroundColor(gameResult.bottomSheetSubheadingColor)
          .padding(.top, 2)

        HStack(spacing: 8) {
          Button {
            onKeepPlayingTapped()
          } label: {
            Text("Keep Playing")
              .font(YralFont.pt16.semiBold.swiftUIFont)
              .foregroundColor(YralColor.grey50.swiftUIColor)
              .frame(maxWidth: .infinity)
              .frame(height: 42)
              .background(Constants.buttonGradient)
              .clipShape(RoundedRectangle(cornerRadius: 8))
          }

          Button {
            onLearnMoreTapped()
          } label: {
            Text("Learn More")
              .font(YralFont.pt16.semiBold.swiftUIFont)
              .foregroundColor(YralColor.primary300.swiftUIColor)
              .frame(maxWidth: .infinity)
              .frame(height: 42)
              .overlay(
                RoundedRectangle(cornerRadius: 8)
                  .stroke(Constants.buttonGradient, lineWidth: 1)
              )
          }
        }
        .padding(.top, 32)
        .padding(.bottom, 16)
      }
      .frame(maxWidth: .infinity, alignment: .bottom)
      .padding(.horizontal, 16)
      .background(YralColor.grey900.swiftUIColor)
    }
  }
}

extension SmileyGameResultBottomSheetView {
  enum Constants {
    static let buttonGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
      ],
      startPoint: .init(x: 0.94, y: 0.13),
      endPoint: .init(x: 0.35, y: 0.89)
    )
  }
}
