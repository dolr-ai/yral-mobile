//
//  VideoInsightsBottomSheet.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct VideoInsightsBottomSheet: View {
  @StateObject var viewModel: VideoInsightsViewModel

  @State private var showBottomSheet = false
  @State private var dragOffset: CGFloat = .zero
  @State private var insightsInfo: VideoInsightsDTO?
  @State private var errorMessage: String?

  private var openedFromFeed: Bool
  private var videoInfo: ProfileVideoInfo
  let onComplete: (Int64?) -> Void

  init(
    viewModel: VideoInsightsViewModel,
    openedFromFeed: Bool,
    videoInfo: ProfileVideoInfo,
    onComplete: @escaping (Int64?) -> Void
  ) {
    self._viewModel = StateObject(wrappedValue: viewModel)
    self.openedFromFeed = openedFromFeed
    self.videoInfo = videoInfo
    self.onComplete = onComplete
  }

  var body: some View {
    ZStack(alignment: .bottom) {
      if openedFromFeed {
        Color.black.opacity(Constants.backgroundOpacity)
          .contentShape(Rectangle())
          .ignoresSafeArea()
          .onTapGesture {
            dismiss()
          }
          .transition(.opacity)
      } else {
        Color.clear
          .contentShape(Rectangle())
          .ignoresSafeArea()
          .onTapGesture {
            dismiss()
          }
          .transition(.opacity)
      }

      if showBottomSheet {
        VStack(spacing: .zero) {
          if let error = errorMessage {
            buildErrorView(errorMessage: error)
          } else {
            buildView()
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
    .onChange(of: viewModel.event) { event in
      switch event {
      case .fetchedVideoInsights(let insights):
        insightsInfo = insights.first
        errorMessage = nil
      case .failedToFetchVideoInsights(let error):
        insightsInfo = nil
        errorMessage = error
      default:
        break
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
    .task {
      await viewModel.fetchVideoInsights(videoIDs: [videoInfo.videoId])
    }
  }

  private func dismiss() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      showBottomSheet = false
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      if let totalViews = insightsInfo?.totalViews {
        onComplete(Int64(totalViews))
      } else {
        onComplete(nil)
      }
    }
  }

  // swiftlint: disable function_body_length
  @ViewBuilder
  private func buildView() -> some View {
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

      URLImage(url: videoInfo.thumbnailUrl)
        .frame(width: Constants.imageWidth, height: Constants.imageHeight)
        .clipShape(RoundedRectangle(cornerRadius: Constants.imageCornerRadius))
        .padding(.bottom, Constants.imageBottom)

      HStack(spacing: .zero) {
        Text(Constants.totalViews)
          .font(Constants.viewTitleFont)
          .foregroundColor(Constants.viewTitleColor)

        Spacer()

        if let totalViews = insightsInfo?.totalViews {
          Text("\(totalViews.formattedWithSuffix)")
            .font(Constants.viewNumberFont)
            .foregroundColor(Constants.viewNumberColor)
        }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(.bottom, Constants.viewHstackBottom)

      HStack(spacing: .zero) {
        Text(Constants.engagedViews)
          .font(Constants.viewTitleFont)
          .foregroundColor(Constants.viewTitleColor)

        Spacer()

        if let engagedViews = insightsInfo?.engagedViews {
          Text("\(engagedViews.formattedWithSuffix)")
            .font(Constants.viewNumberFont)
            .foregroundColor(Constants.viewNumberColor)
        }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(.bottom, Constants.viewHstackBottom)

      HStack(alignment: .top, spacing: Constants.helperHstackSpacing) {
        Image(Constants.helperImage)
          .resizable()
          .frame(width: Constants.helperImageSize, height: Constants.helperImageSize)

        Text(Constants.helperText)
          .font(Constants.helperTextFont)
          .foregroundColor(Constants.helperTextColor)
      }
      .padding(Constants.helperHstackPadding)
      .frame(maxWidth: .infinity, alignment: .leading)
      .background(Constants.helperHstackColor)
      .overlay(
        RoundedRectangle(cornerRadius: Constants.helperHstackCorner)
          .stroke(Constants.helperHstackBorder, lineWidth: .one)
      )
      .cornerRadius(Constants.helperHstackCorner)
      .padding(.bottom, Constants.helperHstackBottom)
    }
    // swiftlint: enable function_body_length
  }

  @ViewBuilder
  private func buildErrorView(errorMessage: String) -> some View {
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

      Image(Constants.errorImage)
        .resizable()
        .frame(width: Constants.errorImageSize, height: Constants.errorImageSize)

      Text(Constants.errorMessage)
        .font(Constants.errorMessageFont)
        .foregroundColor(Constants.errorMessageColor)
        .multilineTextAlignment(.center)
        .padding(.vertical, Constants.errorMessageVertical)

      Button {
        Task {
          await self.viewModel.fetchVideoInsights(videoIDs: [videoInfo.videoId])
        }
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
  }
}

extension VideoInsightsBottomSheet {
  enum Constants {
    static let backgroundColor = YralColor.grey900.swiftUIColor
    static let backgroundOpacity = 0.8
    static let vStackHorizontalPadding = 16.0
    static let bottomSheetDismissValue = 100.0
    static let bottomSheetDismissTime = 0.1
    static let bottomSheetAppearTime = 0.3

    static let handleWidth = 32.0
    static let handleHeight = 2.0
    static let handleColor = YralColor.grey500.swiftUIColor
    static let handleTopPadding = 12.0
    static let handleBottomPadding = 28.0

    static let title = "Video Insights"
    static let titleFont = YralFont.pt18.bold.swiftUIFont
    static let titleColor = YralColor.grey0.swiftUIColor
    static let titleBottom = 28.0

    static let imageWidth = 120.0
    static let imageHeight = 228.0
    static let imageBottom = 28.0
    static let imageCornerRadius = 8.0

    static let viewHstackBottom = 16.0
    static let totalViews = "Total Views"
    static let engagedViews = "Total Engaged Views"

    static let viewTitleFont = YralFont.pt16.medium.swiftUIFont
    static let viewTitleColor = YralColor.grey400.swiftUIColor
    static let viewNumberFont = YralFont.pt16.semiBold.swiftUIFont
    static let viewNumberColor = YralColor.grey50.swiftUIColor

    static let helperImage = "insights_info"
    static let helperImageSize = 18.0

    static let helperText = """
      Engaged views: Counted when a logged-in user watches your video for more than 3 seconds. \
      Each user counts only once per video.
      """
    static let helperTextFont = YralFont.pt12.regular.swiftUIFont
    static let helperTextColor = YralColor.blue100.swiftUIColor

    static let helperHstackSpacing = 6.0
    static let helperHstackPadding = 8.0
    static let helperHstackBottom = 36.0
    static let helperHstackCorner = 8.0
    static let helperHstackColor = Color(hex: "0A1626")
    static let helperHstackBorder = YralColor.blue300.swiftUIColor

    static let errorImage = "upload_error_image"
    static let errorImageSize = 120.0

    static let errorMessage = """
      Oops...Something went wrong!
      We couldn’t load your data. Please refresh or try again later.
      """
    static let errorMessageFont = YralFont.pt16.regular.swiftUIFont
    static let errorMessageColor = YralColor.grey300.swiftUIColor
    static let errorMessageVertical = 30.0

    static let buttonText = "Refresh"
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
