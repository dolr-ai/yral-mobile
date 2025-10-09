//
//  VideoViewedRewardsBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 02/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct VideoViewedRewardsBottomSheet: View {
  @State private var showBottomSheet = false
  @State private var dragOffset: CGFloat = .zero
  @State private var lottieData: Data?

  let onComplete: () -> Void

  private var getBitcoinAttributedText: NSAttributedString {
    let paragraphStyle = NSMutableParagraphStyle()
    paragraphStyle.lineHeightMultiple = Constants.descriptionLineHeight

    let baseFont = Constants.descriptionBaseFont
    let boldFont = Constants.descriptionBoldFont

    let attributed = NSMutableAttributedString()

    let attrs: [NSAttributedString.Key: Any] = [
      .font: baseFont,
      .foregroundColor: Constants.descriptionBaseColor,
      .paragraphStyle: paragraphStyle
    ]

    let boldAttrs: [NSAttributedString.Key: Any] = [
      .font: boldFont,
      .foregroundColor: Constants.descriptionBoldColor,
      .paragraphStyle: paragraphStyle
    ]

    attributed.append(NSAttributedString(string: Constants.descriptionText1, attributes: attrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText2, attributes: boldAttrs))
    attributed.append(NSAttributedString(string: Constants.descriptionText3, attributes: attrs))

    return attributed
  }

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
      FirebaseLottieManager.shared.data(
        forPath: Constants.lottiePath
      ) { result in
          if case .success(let data) = result {
            lottieData = data
          }
        }

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
      Text(Constants.title)
        .font(Constants.titleFont)
        .foregroundColor(Constants.titleColor)
        .padding(.top, Constants.titleTop)
        .padding(.bottom, Constants.titleBottom)

      if let data = lottieData {
        LottieView(
          data: data,
          name: "",
          loopMode: .loop,
          animationSpeed: .one,
          resetProgress: false) {}
          .frame(width: Constants.lottieWidth)
          .frame(height: Constants.lottieHeight)
          .aspectRatio(contentMode: .fit)
      } else {
        Color.clear
          .frame(width: Constants.lottieWidth)
          .frame(height: Constants.lottieHeight)
          .overlay(
            LottieLoaderView(animationName: Constants.loader, resetProgess: false)
              .frame(width: Constants.loaderSize, height: Constants.loaderSize)
          )
      }

      AttributedText(
        attributedString: getBitcoinAttributedText,
        alignment: .center
      )
      .frame(height: Constants.descriptionHeight)
      .padding(.top, Constants.descriptionTop)
      .padding(.bottom, Constants.descriptionBottom)
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      onComplete()
    }
  }
}

extension VideoViewedRewardsBottomSheet {
  enum Constants {
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0
    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3

    static let loader = "Yral_Loader"
    static let loaderSize = 24.0

    static let title = "How to get Bitcoin reward?"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleTop = 40.0
    static let titleBottom = 40.0

    static let lottiePath = "btc_rewards/btc_rewards_views.json"
    static let lottieWidth = UIScreen.main.bounds.width - 32
    static let lottieHeight = lottieWidth * 0.5

    static let descriptionTop = 32.0
    static let descriptionBottom = 40.0
    static let descriptionBaseFont = YralFont.pt16.regular.uiFont
    static let descriptionBoldFont = YralFont.pt16.bold.uiFont
    static let descriptionText1 = "Get "
    static let descriptionText2 = "Bitcoin\n"
    static let descriptionText3 = "on having views per video."
    static let descriptionBaseColor =  YralColor.grey300.uiColor
    static let descriptionBoldColor = UIColor(
      red: 219/255,
      green: 156/255,
      blue: 54/255,
      alpha: 1
    )
    static let descriptionLineHeight = 1.14
    static let descriptionHeight = 44.0
  }
}
