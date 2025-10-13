//
//  VideoViewedRewardsCreditedBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 06/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct VideoViewedRewardsCreditedBottomSheet: View {
  @EnvironmentObject var eventBus: EventBus

  @State private var showBottomSheet = false
  @State private var dragOffset: CGFloat = .zero

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
    attributed.append(NSAttributedString(string: Constants.descriptionText4, attributes: attrs))

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

      Image(Constants.btcImage)
        .resizable()
        .aspectRatio(contentMode: .fit)

      AttributedText(
        attributedString: getBitcoinAttributedText,
        horizontalPadding: Constants.vStackHorizontalPadding,
        alignment: .center
      )
      .frame(height: Constants.descriptionHeight)
      .padding(.bottom, Constants.descriptionBottom)

      Button {
        dismiss()
        eventBus.walletTapped.send(())
      } label: {
        Text(Constants.walletText)
          .font(Constants.walletFont)
          .foregroundColor(Constants.walletColor)
          .frame(maxWidth: .infinity)
          .frame(height: Constants.walletHeight)
          .background(Constants.walletGradient)
          .clipShape(
            RoundedRectangle(cornerRadius: Constants.walletCornerRadius)
          )
      }
      .padding(.bottom, Constants.walletBottom)

      Button {
        dismiss()
        eventBus.startPlayingTapped.send(())
      } label: {
        Text(Constants.scrollingText)
          .font(Constants.scrollingFont)
          .foregroundColor(Constants.scrollingColor)
          .frame(maxWidth: .infinity)
          .frame(height: Constants.scrollingHeight)
          .background(Constants.scrollingBackground)
          .cornerRadius(Constants.scrollingCornerRadius)
          .overlay(
            RoundedRectangle(cornerRadius: Constants.scrollingCornerRadius)
              .stroke(Constants.scrollingBorder, lineWidth: .one)
          )
      }
      .padding(.bottom, Constants.walletBottom)
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

extension VideoViewedRewardsCreditedBottomSheet {
  enum Constants {
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let vStackHorizontalPadding = 16.0
    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3

    static let title = "Bitcoin Reward added"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleTop = 36.0
    static let titleBottom = 44.0

    static let btcImage = "btc_credited"

    static let descriptionBottom = 28.0
    static let descriptionBaseFont = YralFont.pt16.regular.uiFont
    static let descriptionBoldFont = YralFont.pt16.bold.uiFont
    static let descriptionText1 = "Congrats! You've received a "
    static let descriptionText2 = "Bitcoin "
    static let descriptionText3 = "reward for your video views. "
    static let descriptionText4 = "See it in your wallet."
    static let descriptionBaseColor =  YralColor.grey300.uiColor
    static let descriptionBoldColor = UIColor(
      red: 219/255,
      green: 156/255,
      blue: 54/255,
      alpha: 1
    )
    static let descriptionLineHeight = 1.14
    static let descriptionHeight = 66.0

    static let walletText = "Go to Wallet"
    static let walletFont = YralFont.pt16.bold.swiftUIFont
    static let walletColor = YralColor.grey50.swiftUIColor
    static let walletHeight = 44.0
    static let walletCornerRadius = 8.0
    static let walletBottom = 12.0
    static let walletGradient = LinearGradient(
      stops: [
        .init(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0),
        .init(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        .init(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1)
      ],
      startPoint: .init(x: 0.94, y: 0.13),
      endPoint: .init(x: 0.35, y: 0.89)
    )

    static let scrollingText = "Keep Scrolling"
    static let scrollingFont = YralFont.pt16.medium.swiftUIFont
    static let scrollingColor = YralColor.grey50.swiftUIColor
    static let scrollingBackground = YralColor.grey800.swiftUIColor
    static let scrollingBorder = YralColor.grey700.swiftUIColor
    static let scrollingHeight = 44.0
    static let scrollingCornerRadius = 8.0
    static let scrollingBottom = 44.0
  }
}
