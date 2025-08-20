//
//  DisableBackBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 20/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct DisableBackBottomSheet: View {
  let onDismiss: () -> Void
  let onStayHere: () -> Void

  @State private var dragOffset: CGFloat = .zero
  @State private var showBottomSheet = false

  var body: some View {
    ZStack(alignment: .bottom) {
      Color.black.opacity(Constants.backgroundOpacity)
        .ignoresSafeArea()
        .onTapGesture {
          stayHere()
        }
        .transition(.opacity)

      if showBottomSheet {
        VStack(spacing: .zero) {
          RoundedRectangle(cornerRadius: Constants.handleHeight / .two)
            .frame(width: Constants.handleWidth, height: Constants.handleHeight)
            .foregroundColor(Constants.handleColor)
            .padding(.top, Constants.handleTopPadding)
            .padding(.bottom, Constants.handleBottomPadding)

          Text(Constants.title)
            .font(Constants.titleFont)
            .foregroundColor(Constants.titleColor)
            .padding(.bottom, Constants.titleBottom)

          Text(Constants.subtitle)
            .font(Constants.subtitleFont)
            .foregroundColor(Constants.subtitleColor)
            .padding(.bottom, Constants.subtitleBottom)

          HStack(spacing: Constants.hstackSpacing) {
            Button {
              dismiss()
            } label: {
              Text(Constants.dismissText)
                .font(Constants.dismissTextFont)
                .foregroundColor(Constants.dismissTextColor)
                .frame(maxWidth: .infinity)
                .frame(height: Constants.dismissTextHeight)
                .background(Constants.dismissTextBackground)
                .cornerRadius(Constants.dismissTextCornerRadius)
                .overlay(
                  RoundedRectangle(cornerRadius: Constants.dismissTextCornerRadius)
                    .stroke(Constants.dismissTextBorderColor, lineWidth: .one)
                )
            }

            Button {
              stayHere()
            } label: {
              Text(Constants.stayHereText)
                .font(Constants.stayHereTextFont)
                .foregroundColor(Constants.stayHereTextColor)
                .frame(maxWidth: .infinity)
                .frame(height: Constants.stayHereTextHeight)
                .background(Constants.stayHereGradient)
                .cornerRadius(Constants.stayHereTextCornerRadius)
            }
          }
          .padding(.bottom, Constants.hstackBottom)
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
                stayHere()
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
      onDismiss()
    }
  }

  private func stayHere() {
    withAnimation(.easeInOut(duration: Constants.bottomSheetAppearTime)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.bottomSheetAppearTime) {
      onStayHere()
    }
  }
}

extension DisableBackBottomSheet {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0

    static let handleWidth = 32.0
    static let handleHeight = 2.0
    static let handleColor = YralColor.grey500.swiftUIColor
    static let handleTopPadding = 12.0
    static let handleBottomPadding = 28.0

    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3

    static let title = "You will loose AI Credits"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleBottom = 16.0

    static let subtitle = "Leaving now will use up today’s AI credit. You can return tomorrow for a fresh credit."
    static let subtitleFont = YralFont.pt16.medium.swiftUIFont
    static let subtitleColor = YralColor.grey0.swiftUIColor
    static let subtitleBottom = 32.0

    static let hstackSpacing = 12.0
    static let hstackBottom = 16.0

    static let dismissText = "Yes, Take me back"
    static let dismissTextFont = YralFont.pt16.medium.swiftUIFont
    static let dismissTextColor = YralColor.grey50.swiftUIColor
    static let dismissTextBackground = YralColor.grey800.swiftUIColor
    static let dismissTextCornerRadius = 8.0
    static let dismissTextBorderColor = YralColor.grey700.swiftUIColor
    static let dismissTextHeight = 42.0

    static let stayHereText = "Stay Here"
    static let stayHereTextFont = YralFont.pt16.medium.swiftUIFont
    static let stayHereTextColor = YralColor.grey50.swiftUIColor
    static let stayHereTextCornerRadius = 8.0
    static let stayHereTextHeight = 42.0
    static let stayHereGradient = LinearGradient(
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
