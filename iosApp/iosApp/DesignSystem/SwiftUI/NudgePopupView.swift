//
//  NudgePopupView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct NudgePopupView: View {
  let nudgeTitle: String
  let nudgeMessage: String
  let confirmLabel: String
  let cancelLabel: String

  let onConfirm: () -> Void
  let onCancel: () -> Void

  @State private var dragOffset: CGFloat = .zero
  @State private var showContainer: Bool = false

  var body: some View {
    GeometryReader { geometry in
      let bottomInset = geometry.safeAreaInsets.bottom
      ZStack(alignment: .bottom) {
        Color.black.opacity(Constants.backgroundOpacity)
          .edgesIgnoringSafeArea(.all)
          .onTapGesture { dismiss() }
          .transition(.opacity)

        if showContainer {
          VStack(alignment: .center, spacing: Constants.containerSpacing) {
            Rectangle()
              .fill(Constants.indicatorColor)
              .frame(width: Constants.indicatorWidth, height: Constants.indicatorHeight)
              .cornerRadius(Constants.indicatorCornerRadius)
              .offset(y: Constants.indicatorOffsetY)

            VStack(alignment: .center, spacing: Constants.outerVStackSpacing) {
              VStack(alignment: .center, spacing: Constants.innerVStackSpacing) {
                Text(nudgeTitle)
                  .font(Constants.nudgeTitleFont)
                  .foregroundColor(Constants.nudgeTitleTextColor)

                Text(nudgeMessage)
                  .font(Constants.nudgeMessageFont)
                  .foregroundColor(Constants.nudgeMessageColor)
                  .multilineTextAlignment(.center)
              }

              HStack {
                Button(
                  action: { dismiss() },
                  label: {
                    Text(cancelLabel)
                      .font(Constants.buttonFont)
                      .foregroundColor(Constants.buttonTextColor)
                      .frame(maxWidth: .infinity)
                      .frame(height: Constants.buttonHeight)
                      .background(Constants.leftButtonBackgroundColor)
                      .cornerRadius(Constants.buttonCornerRadius)
                      .overlay(
                        RoundedRectangle(cornerRadius: Constants.buttonCornerRadius)
                          .stroke(Constants.leftButtonBorderColor, lineWidth: .one)
                      )
                  }
                )

                Divider()
                  .frame(height: Constants.buttonHeight)

                Button(
                  action: { dismiss() },
                  label: {
                    Text(confirmLabel)
                      .font(Constants.buttonFont)
                      .foregroundColor(Constants.buttonTextColor)
                      .frame(maxWidth: .infinity)
                      .frame(height: Constants.buttonHeight)
                      .background(Constants.rightButtonBackgroundColor)
                      .cornerRadius(Constants.buttonCornerRadius)
                  }
                )
              }
            }
            .padding(.horizontal, Constants.outerVStackHorizontalPadding)
            .padding(.bottom, Constants.outerVStackBottomPadding + bottomInset + Constants.containerOffset)
          }
          .background(Constants.sheetBackgroundColor)
          .cornerRadius(Constants.popupCornerRadius)
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
      .onAppear {
        withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
          showContainer = true
        }
      }
      .onDisappear {
        UIView.setAnimationsEnabled(true)
      }
      .edgesIgnoringSafeArea(.all)
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showContainer = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      onCancel()
    }
  }
}

extension NudgePopupView {
  enum Constants {
    static let sheetBackgroundColor = YralColor.grey900.swiftUIColor
    static let indicatorColor = YralColor.grey500.swiftUIColor
    static let indicatorWidth = 32.0
    static let indicatorHeight = 2.0
    static let indicatorCornerRadius = 12.0
    static let indicatorOffsetY: CGFloat = 12.0
    static let popupCornerRadius: CGFloat = 20
    static let horizontalPadding: CGFloat = 16
    static let nudgeTitleFont = YralFont.pt18.bold.swiftUIFont
    static let nudgeTitleTextColor = YralColor.grey0.swiftUIColor
    static let nudgeMessageFont = YralFont.pt16.medium.swiftUIFont
    static let nudgeMessageColor = YralColor.grey0.swiftUIColor
    static let buttonFont = YralFont.pt16.medium.swiftUIFont
    static let buttonTextColor = YralColor.grey50.swiftUIColor
    static let leftButtonBackgroundColor = YralColor.grey800.swiftUIColor
    static let leftButtonBorderColor = YralColor.grey700.swiftUIColor
    static let rightButtonBackgroundColor = YralColor.red300.swiftUIColor
    static let backgroundOpacity: CGFloat = 0.8
    static let buttonHeight: CGFloat = 44
    static let buttonCornerRadius: CGFloat = 8
    static let containerSpacing: CGFloat = 38.0
    static let containerOffset: CGFloat = 15.0
    static let outerVStackSpacing: CGFloat = 32.0
    static let outerVStackHorizontalPadding = 16.0
    static let outerVStackBottomPadding: CGFloat = 36.0
    static let innerVStackSpacing: CGFloat = 16.0
  }
}
