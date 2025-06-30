//
//  NotificationsNudge.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 27/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct NotificationsNudge: View {
  let onComplete: () -> Void
  @State private var showCard = false
  @State private var dragOffset: CGFloat = .zero
  @StateObject private var notificationVM = NotificationToggleViewModel()

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

      Image(Constants.imageName)
        .resizable()
        .frame(width: Constants.imageSize, height: Constants.imageSize)
        .padding(.top, Constants.imagePadding)

      Text(Constants.subtitltText)
        .font(Constants.subtitleFont)
        .foregroundStyle(Constants.subtitleTextColor)
        .multilineTextAlignment(.center)
        .padding(.top, Constants.subtitlePadding)

      VStack(alignment: .center, spacing: Constants.vStackSpacing) {
        Button {
          notificationVM.togglePermission(to: true)
          dismiss()
        } label: {
          Text(Constants.turnonButtonText)
            .font(Constants.turnOnButtonFont)
            .foregroundStyle(Constants.turnOnButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
            .background(Constants.turnOnButtonGradient)
            .cornerRadius(Constants.buttonCornerRadius)
        }
        Button {
          dismiss()
        } label: {
          Text(Constants.notNowButtonText)
            .font(Constants.notNowButtonFont)
            .foregroundStyle(Constants.notNowButtonTextColor)
            .frame(maxWidth: .infinity, minHeight: Constants.buttonHeight)
            .background(Constants.notNowButtonBackgoundColor)
            .cornerRadius(Constants.buttonCornerRadius)
            .overlay(
              RoundedRectangle(cornerRadius: Constants.buttonCornerRadius)
                .inset(by: .half)
                .stroke(Constants.notNowButtonBorderColor, lineWidth: .one)
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

extension NotificationsNudge {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let bottomPadding: CGFloat = 16
    static let verticalPadding = 36.0
    static let horizontalPadding = 16.0
    static let imagePadding = 46.0
    static let imageSize = 134.0
    static let subtitlePadding = 46.0
    static let vStackPadding = 28.0
    static let vStackSpacing = 12.0
    static let containerOffset: CGFloat = 15.0

    static let cardCornerRadius: CGFloat = 20
    static let cardShadowRadius: CGFloat = 8

    static let illustrationSize: CGFloat = 152
    static let cardBackgroundColor = YralColor.grey900.swiftUIColor
    static let dimmedBackgroundOpacity = 0.8
    static let appearAnimation = Animation.easeInOut(duration: CGFloat.animationPeriod)
    static let loadingStateOpacity = 0.4
    static let buttonHeight = 45.0
    static let buttonCornerRadius = 8.0

    static let titleText = "A little nudge!"
    static let subtitltText = """
The action moves fast.
    Don’t fall behind, enable notifications and be the first to know when something’s happening.
"""
    static let turnonButtonText = "Turn on Alerts"
    static let notNowButtonText = "Not Now"

    static let imageName = "notification_nudge"
    static let titleFont = YralFont.pt20.semiBold.swiftUIFont
    static let titleTextColor = YralColor.grey0.swiftUIColor
    static let subtitleFont = YralFont.pt14.swiftUIFont
    static let subtitleTextColor = YralColor.grey50.swiftUIColor
    static let turnOnButtonFont = YralFont.pt16.bold.swiftUIFont
    static let turnOnButtonTextColor = YralColor.grey50.swiftUIColor
    static let notNowButtonFont = YralFont.pt14.medium.swiftUIFont
    static let notNowButtonTextColor = YralColor.grey50.swiftUIColor
    static let notNowButtonBackgoundColor = YralColor.grey800.swiftUIColor
    static let notNowButtonBorderColor = YralColor.grey700.swiftUIColor
    static let turnOnButtonGradient = LinearGradient(
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
