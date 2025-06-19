//
//  SmileyGameResultBottomSheetView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct SmileyGameResultBottomSheetView: View {
  var gameResult: SmileyGameResultResponse

  let onKeepPlayingTapped: () -> Void
  let onLearnMoreTapped: () -> Void

  @State private var dragOffset: CGFloat = .zero
  @State private var showBottomSheet = false

  var body: some View {
    ZStack(alignment: .bottom) {
      Color.black.opacity(Constants.backgroundOpacity)
        .ignoresSafeArea()
        .onTapGesture {
          dismiss()
        }
        .transition(.opacity)

      if showBottomSheet {
        VStack(spacing: .zero) {
          RoundedRectangle(cornerRadius: Constants.handleHeight / .two)
            .frame(width: Constants.handleWidth, height: Constants.handleHeight)
            .foregroundColor(Constants.handleColor)
            .padding(.top, Constants.handleTopPadding)

          Text(gameResult.bottomSheetHeading)
            .font(Constants.headingFont)
            .foregroundColor(Constants.headingTextColor)
            .padding(.top, Constants.headingTopPadding)

          LottieView(
            name: gameResult.lottieName,
            loopMode: .playOnce,
            animationSpeed: .one
          ) {}
            .frame(width: Constants.lottieWidht)
            .frame(height: Constants.lottieHeight)

          HStack(spacing: Constants.hStackSpacing) {
            Text(gameResult.bottomSheetTitle)
              .font(Constants.titleFont)
              .foregroundColor(Constants.titleTextColor)

            FirebaseImageView(path: gameResult.smiley.imageURL)
              .frame(width: Constants.imageViewSize, height: Constants.imageViewSize)
          }

          Text(gameResult.bottomSheetSubheading)
            .font(Constants.subheadingFont)
            .foregroundColor(gameResult.bottomSheetSubheadingColor)
            .padding(.top, Constants.subheadingTopPadding)

          HStack(spacing: Constants.hStackSpacing) {
            Button {
              dismiss()
            } label: {
              Text(Constants.keepPlaying)
                .font(Constants.keepPlayingFont)
                .foregroundColor(Constants.keepPlayingTextColor)
                .frame(maxWidth: .infinity)
                .frame(height: Constants.ctaHeight)
                .background(Constants.buttonGradient)
                .clipShape(RoundedRectangle(cornerRadius: Constants.ctaCornerRadius))
            }

            Button {
              learnMore()
            } label: {
              Text(Constants.learnMore)
                .font(Constants.learnMoreFont)
                .foregroundColor(Constants.learnMoreTextColor)
                .frame(maxWidth: .infinity)
                .frame(height: Constants.ctaHeight)
                .overlay(
                  RoundedRectangle(cornerRadius: Constants.ctaCornerRadius)
                    .stroke(Constants.buttonGradient, lineWidth: .one)
                )
            }
          }
          .padding(.top, Constants.hStackTopPadding)
          .padding(.bottom, Constants.hStackBottomPadding)
        }
        .frame(maxWidth: .infinity, alignment: .bottom)
        .padding(.horizontal, Constants.vStackHorizontalPadding)
        .background(Constants.backgroundColor)
        .offset(y: dragOffset)
        .gesture(
          DragGesture()
            .onChanged { value in
              dragOffset = max(value.translation.height, .zero)
            }
            .onEnded { value in
              if value.translation.height > Constants.bottomSheetDismissValue {
                dismiss()
              } else {
                withAnimation(.easeInOut(duration: Constants.bottomSheetDismissTime)) {
                  dragOffset = .zero
                }
              }
            }
        )
        .transition(.move(edge: .bottom))
      }
    }
    .onAppear {
      withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
        showBottomSheet = true
      }
    }
    .onDisappear {
      UIView.setAnimationsEnabled(true)
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.bottomSheetAppearTime) {
      onKeepPlayingTapped()
    }
  }

  private func learnMore() {
    withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.bottomSheetAppearTime) {
      onLearnMoreTapped()
    }
  }
}

extension SmileyGameResultBottomSheetView {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0

    static let handleWidth = 32.0
    static let handleHeight = 2.0
    static let handleColor = YralColor.grey500.swiftUIColor
    static let handleTopPadding = 12.0

    static let headingFont = YralFont.pt18.bold.swiftUIFont
    static let headingTextColor = YralColor.grey0.swiftUIColor
    static let headingTopPadding = 28.0

    static let lottieWidht = 250.0
    static let lottieHeight = 130.0

    static let hStackSpacing = 8.0
    static let hStackTopPadding = 32.0
    static let hStackBottomPadding = 16.0

    static let titleFont = YralFont.pt16.medium.swiftUIFont
    static let titleTextColor = YralColor.green50.swiftUIColor
    static let imageViewSize = 28.0

    static let subheadingFont = YralFont.pt18.bold.swiftUIFont
    static let subheadingTopPadding = 2.0

    static let keepPlaying = "Keep Playing"
    static let keepPlayingFont = YralFont.pt16.semiBold.swiftUIFont
    static let keepPlayingTextColor = YralColor.grey50.swiftUIColor

    static let learnMore = "Learn More"
    static let learnMoreFont = YralFont.pt16.semiBold.swiftUIFont
    static let learnMoreTextColor = YralColor.primary300.swiftUIColor

    static let ctaHeight = 42.0
    static let ctaCornerRadius = 8.0

    static let buttonGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
      ],
      startPoint: .init(x: 0.94, y: 0.13),
      endPoint: .init(x: 0.35, y: 0.89)
    )

    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3
  }
}

extension SmileyGameResultResponse {
  var bottomSheetHeading: String {
    outcome == "WIN" ? "Congratulations!" : "OOPS!!!"
  }

  var bottomSheetTitle: String {
    outcome == "WIN" ? "Since most people voted on" : "Since most people didn't voted on"
  }

  var bottomSheetSubheading: String {
    outcome == "WIN" ? "You Won \(abs(coinDelta)) Points" : "You Lost \(abs(coinDelta)) Points"
  }

  var bottomSheetSubheadingColor: Color {
    outcome == "WIN" ? YralColor.green300.swiftUIColor : YralColor.red300.swiftUIColor
  }

  var lottieName: String {
    outcome == "WIN" ? "smiley_game_win" : "smiley_game_lose"
  }
}
