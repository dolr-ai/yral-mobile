//
//  RecommendedUpdatePopUp.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct RecommendedUpdatePopUp: View {
  let onComplete: () -> Void
  @State private var showCard = false
  @State private var dragOffset: CGFloat = .zero

  var body: some View {
    GeometryReader { geometry in
      let bottomInset = geometry.safeAreaInsets.bottom
      ZStack(alignment: .bottom) {
        Color.black.opacity(Constants.backgroundOpacity)
          .edgesIgnoringSafeArea(.all)
          .onTapGesture { dismiss() }
          .transition(.opacity)

        if showCard {
          VStack(alignment: .center, spacing: .zero) {
            buildNudge()
          }
          .padding(.horizontal, Constants.horizontalPadding)
          .padding(.bottom, Constants.verticalPadding + bottomInset + Constants.containerOffset)
          .background(Constants.cardBackgroundColor)
          .cornerRadius(Constants.cardCornerRadius)
          // to hide bottom corner radius for older devices with no bottom safe area
            .offset(y: Constants.containerOffset + dragOffset)
            .gesture(
              DragGesture()
                .onChanged { value in
                  dragOffset = max(value.translation.height, 0)
                }
                .onEnded { value in
                  if value.translation.height > 100 {
                    dismiss()
                  } else {
                    withAnimation(.easeInOut(duration: CGFloat.pointOne)) {
                      dragOffset = .zero
                    }
                  }
                }
            )
            .transition(.move(edge: .bottom))
        }
      }
    }
    .onAppear {
      withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
        showCard = true
      }
    }
    .onDisappear {
      UIView.setAnimationsEnabled(true)
    }
    .edgesIgnoringSafeArea(.all)
  }

  @ViewBuilder private func buildNudge() -> some View {
    VStack(alignment: .center, spacing: .zero) {
      Text(Constants.titleText)
        .font(Constants.titleFont)
        .foregroundStyle(Constants.titleTextColor)
        .padding(.top, Constants.verticalPadding)

      LottieLoaderView(animationName: Constants.lottieName, loopMode: .playOnce)
        .frame(width: Constants.imageSize, height: Constants.imageSize)
        .padding(.top, Constants.imagePadding)

      Text(Constants.subtitleText)
        .font(Constants.subtitleFont)
        .foregroundStyle(Constants.subtitleTextColor)
        .multilineTextAlignment(.center)
        .padding(.top, Constants.subtitlePadding)

      VStack(alignment: .center, spacing: Constants.buttonVStackSpacing) {
        Button {
          dismiss()
        } label: {
          Text(Constants.updateButtonText)
            .font(Constants.updateButtonFont)
            .foregroundStyle(Constants.updateButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
            .background(Constants.updateButtonGradient)
            .cornerRadius(Constants.buttonCornerRadius)
        }
        Button {
          dismiss()
        } label: {
          Text(Constants.cancelButtonText)
            .font(Constants.cancelButtonFont)
            .foregroundStyle(Constants.cancelButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
            .background(Constants.cancelButtonBGColor)
            .cornerRadius(Constants.buttonCornerRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.buttonCornerRadius)
                .stroke(Constants.cancelButtonBorderColor, lineWidth: CGFloat.one)
            )
        }
      }
      .padding(.top, Constants.vStackPadding)
      .padding(.bottom, Constants.vStackPadding)
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showCard = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      onComplete()
    }
  }
}

extension RecommendedUpdatePopUp {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let bottomPadding: CGFloat = 16
    static let verticalPadding = 36.0
    static let horizontalPadding = 16.0
    static let imagePadding = 46.0
    static let imageSize = 134.0
    static let subtitlePadding = 46.0
    static let vStackPadding = 28.0
    static let containerOffset: CGFloat = 15.0

    static let cardCornerRadius: CGFloat = 16
    static let cardShadowRadius: CGFloat = 8

    static let illustrationSize: CGFloat = 134.0
    static let cardBackgroundColor = YralColor.grey900.swiftUIColor
    static let dimmedBackgroundOpacity = 0.8
    static let appearAnimation = Animation.easeInOut(duration: CGFloat.animationPeriod)
    static let buttonHeight = 45.0
    static let buttonCornerRadius = 8.0
    static let buttonVStackSpacing = 16.0

    static let titleText = "Update Available"
    // swiftlint: disable line_length
    static let subtitleText = "We waved our magic wand and poof! Your app is now smoother, faster, and shinier. Update to unlock the magic!"
    // swiftlint: enable line_length
    static let updateButtonText = "Update Now"
    static let cancelButtonText = "Remind Later"

    static let lottieName = "app_update_animation"
    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let titleTextColor = YralColor.grey0.swiftUIColor
    static let subtitleFont = YralFont.pt14.swiftUIFont
    static let subtitleTextColor = YralColor.grey50.swiftUIColor
    static let updateButtonFont = YralFont.pt16.bold.swiftUIFont
    static let updateButtonTextColor = YralColor.grey50.swiftUIColor
    static let cancelButtonFont = YralFont.pt16.medium.swiftUIFont
    static let cancelButtonTextColor = YralColor.grey50.swiftUIColor
    static let cancelButtonBGColor = YralColor.grey800.swiftUIColor
    static let cancelButtonBorderColor = YralColor.grey700.swiftUIColor
    static let updateButtonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
  }
}
