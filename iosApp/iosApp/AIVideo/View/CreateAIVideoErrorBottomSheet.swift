//
//  CreateAIVideoErrorBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct CreateAIVideoErrorBottomSheet: View {
  let text: String
  let onDismiss: () -> Void

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
          Text(Constants.title)
            .font(Constants.titleFont)
            .foregroundColor(Constants.titleColor)
            .padding(.top, Constants.titleTop)
            .padding(.bottom, Constants.titleBottom)

          Image(Constants.image)
            .resizable()
            .frame(width: Constants.imageSize, height: Constants.imageSize)

          Text(text)
            .font(Constants.errorFont)
            .foregroundColor(Constants.errorColor)
            .padding(.vertical, Constants.errorVertical)

          Button {
            dismiss()
          } label: {
            Text(Constants.buttonText)
              .foregroundColor(Constants.buttonTextColor)
              .font(Constants.buttonFont)
              .frame(maxWidth: .infinity)
              .frame(height: Constants.buttonHeight)
              .background(Constants.buttonGradient)
              .cornerRadius(Constants.buttonCornerRadius)
          }
          .padding(.bottom, Constants.buttonBottom)
        }
        .frame(maxWidth: .infinity, alignment: .bottom)
        .padding(.horizontal, Constants.vStackHorizontalPadding)
        .background(Constants.backgroundColor)
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
}

extension CreateAIVideoErrorBottomSheet {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0
    static let bottomSheetAppearTime = 0.3

    static let title = "Something went wrong!"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleTop = 36.0
    static let titleBottom = 46.0

    static let image = "upload_error_image"
    static let imageSize = 120.0

    static let errorFont = YralFont.pt14.regular.swiftUIFont
    static let errorColor = YralColor.grey300.swiftUIColor
    static let errorVertical = 28.0

    static let buttonText = "Try Again"
    static let buttonTextColor = YralColor.grey50.swiftUIColor
    static let buttonFont = YralFont.pt16.bold.swiftUIFont
    static let buttonHeight = 45.0
    static let buttonCornerRadius = 8.0
    static let buttonBottom = 36.0
    static let buttonGradient = LinearGradient(
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
