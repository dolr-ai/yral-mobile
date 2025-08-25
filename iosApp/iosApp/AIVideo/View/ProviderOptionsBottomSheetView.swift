//
//  ProviderOptionsBottomSheetView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProviderOptionsBottomSheetView: View {
  let providers: [AIVideoProviderResponse]
  let updateSelectedProvier: (AIVideoProviderResponse) -> Void
  let onDismiss: () -> Void

  @State private var dragOffset: CGFloat = .zero
  @State private var showBottomSheet = false
  @State private var selectedProviderID: String = ""

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
            .padding(.bottom, Constants.handleBottomPadding)

          ForEach(providers) { provider in
            buildProviderView(provider: provider)
              .frame(maxWidth: .infinity, alignment: .leading)
              .background(
                provider.id == selectedProviderID ?
                Constants.selectedBackground.cornerRadius(Constants.selectedBackgroundCornerRadius) :
                nil
              )
              .padding(.horizontal, Constants.hstackHorizontal)
              .padding(.bottom, Constants.hstackBottom)
              .onTapGesture {
                selectedProviderID = provider.id
                updateSelectedProvier(provider)
                dismiss()
              }
          }
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
      onDismiss()
    }
  }

  @ViewBuilder
  func buildProviderView(provider: AIVideoProviderResponse) -> some View {
    HStack(spacing: Constants.hstackSpacing) {
      Image(provider.id == selectedProviderID ? Constants.selectedImage : Constants.unselectedImage)
        .resizable()
        .frame(width: Constants.selectorImageSize, height: Constants.selectorImageSize)

      VStack(alignment: .leading, spacing: Constants.vstackSpacing) {
        HStack(spacing: Constants.internalHstackSpacing) {
          URLImage(url: URL(string: provider.iconURL))
            .frame(width: Constants.modelImageSize, height: Constants.modelImageSize)

          Text(provider.name)
            .font(Constants.modelNameFont)
            .foregroundColor(Constants.modelNameColor)
        }

        Text(provider.description)
          .font(Constants.modelDescriptionFont)
          .foregroundColor(Constants.modelDescriptionColor)

        HStack(spacing: Constants.durationHstackSpacing) {
          Image(Constants.durationImage)
            .resizable()
            .frame(width: Constants.durationImageSize, height: Constants.durationImageSize)

          Text("\(provider.defaultDuration) Sec")
            .font(Constants.durationFont)
            .foregroundColor(Constants.durationColor)
        }
        .padding(Constants.durationHstackPadding)
        .cornerRadius(Constants.durationHStackCornerRadius)
        .overlay(
          RoundedRectangle(cornerRadius: Constants.durationHStackCornerRadius)
            .stroke(Constants.durationHStackBorderColor, lineWidth: Constants.durationHStackBorderWidth)
        )
      }
    }
    .padding(.vertical, Constants.hstackVertical)
  }
}

extension ProviderOptionsBottomSheetView {
  enum Constants {
    static let backgroundOpacity = 0.8
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0

    static let handleWidth = 32.0
    static let handleHeight = 2.0
    static let handleColor = YralColor.grey500.swiftUIColor
    static let handleTopPadding = 12.0
    static let handleBottomPadding = 28.0

    static let hstackSpacing = 16.0
    static let hstackHorizontal = 16.0
    static let hstackVertical = 12.0
    static let hstackBottom = 12.0

    static let selectedImage = "provider_selected"
    static let unselectedImage = "provider_unselected"
    static let selectorImageSize = 18.0
    static let selectedBackground = YralColor.grey800.swiftUIColor
    static let selectedBackgroundCornerRadius = 6.0

    static let vstackSpacing = 10.0

    static let internalHstackSpacing = 10.0

    static let durationHstackSpacing = 4.0
    static let durationHstackPadding = 4.0
    static let durationHStackCornerRadius = 4.0
    static let durationHStackBorderColor = YralColor.grey50.swiftUIColor
    static let durationHStackBorderWidth = 0.5

    static let modelNameFont = YralFont.pt16.medium.swiftUIFont
    static let modelNameColor = YralColor.grey50.swiftUIColor

    static let modelImageSize = 30.0
    static let modelDescriptionFont = YralFont.pt14.regular.swiftUIFont
    static let modelDescriptionColor = YralColor.grey50.swiftUIColor

    static let durationImage = "provider_duration"
    static let durationImageSize = 16.0
    static let durationFont = YralFont.pt12.regular.swiftUIFont
    static let durationColor = YralColor.grey400.swiftUIColor

    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3
  }
}
