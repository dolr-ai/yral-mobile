//
//  ConfirmUsernameBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ConfirmUsernameBottomSheet: View {
  @State private var showBottomSheet = false
  @State private var dragOffset: CGFloat = .zero

  let newUsername: String
  let onCancel: () -> Void
  let onChangeName: () -> Void

  var body: some View {
    ZStack(alignment: .bottom) {
      Color.clear
        .contentShape(Rectangle())
        .ignoresSafeArea()
        .onTapGesture {
          dismiss()
        }
        .transition(.opacity)

      if showBottomSheet {
        VStack(spacing: .zero) {
          buildNudge()
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

  @ViewBuilder private func buildNudge() -> some View {
    VStack(alignment: .center, spacing: .zero) {
      Text(Constants.usernameText + newUsername + "?")
        .font(Constants.usernameFont)
        .foregroundColor(Constants.usernameColor)
        .multilineTextAlignment(.center)
        .padding(.top, Constants.usernameTop)
        .padding(.bottom, Constants.usernameBottom)

      Button {

      }
      label: {
        Text(Constants.changeButtonTitle)
          .foregroundColor(Constants.changeButtonTextColor)
          .font(Constants.changeButtonFont)
          .frame(maxWidth: .infinity, minHeight: Constants.changeButtonHeight)
          .background(Constants.changeButtonGradient)
          .cornerRadius(Constants.changeButtonCornerRadius)
      }
      .padding(.bottom, Constants.changeButtonBottom)

      Button {

      } label: {
        Text(Constants.cancelButtonTitle)
          .font(Constants.cancelButtonFont)
          .foregroundColor(Constants.cancelButtonColor)
          .frame(maxWidth: .infinity, alignment: .center)
          .frame(height: Constants.cancelButtonHeight)
          .background(
            RoundedRectangle(cornerRadius: Constants.cancelButtonCornerRadius)
              .fill(Constants.cancelButtonBackground)
          )
          .overlay(
            RoundedRectangle(cornerRadius: Constants.cancelButtonCornerRadius)
              .stroke(Constants.cancelButtonBorder, lineWidth: .one)
          )
      }
      .padding(.bottom, Constants.cancelButtonBottom)
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      onCancel()
    }
  }
}

extension ConfirmUsernameBottomSheet {
  enum Constants {
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0
    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3

    static let usernameFont = YralFont.pt20.semiBold.swiftUIFont
    static let usernameColor = YralColor.grey0.swiftUIColor
    static let usernameTop = 36.0
    static let usernameBottom = 40.0
    static let usernameText = "Are you sure you want to change your name to "

    static let changeButtonTitle = "Change name"
    static let changeButtonTextColor =  YralColor.grey50.swiftUIColor
    static let changeButtonFont = YralFont.pt16.bold.swiftUIFont
    static let changeButtonGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let changeButtonHeight = 45.0
    static let changeButtonCornerRadius: CGFloat = 8
    static let changeButtonBottom = 12.0

    static let cancelButtonTitle = "Cancel"
    static let cancelButtonFont = YralFont.pt14.semiBold.swiftUIFont
    static let cancelButtonColor = YralColor.grey50.swiftUIColor
    static let cancelButtonBackground = YralColor.grey800.swiftUIColor
    static let cancelButtonBorder = YralColor.grey700.swiftUIColor
    static let cancelButtonHeight = 42.0
    static let cancelButtonCornerRadius = 8.0
    static let cancelButtonBottom = 40.0
  }
}
