//
//  WalletView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

struct WalletView: View {
  @StateObject var viewModel: WalletViewModel
  @State var accountInfo: AccountInfo?
  @State private var isLoadingFirstTime = true
  @State private var yralToken = 0
  @State private var btcToken = 0.0
  @State private var btcToCurrencyValue = 0.0
  @State private var showEarnBTCButton = false
  @State private var showEarnBTCBottomSheet = false
  @EnvironmentObject var session: SessionManager

  init(viewModel: WalletViewModel) {
    _viewModel = StateObject(wrappedValue: viewModel)
  }

  var body: some View {
    VStack(spacing: .zero) {
      VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
        Text(Constants.navigationTitle)
          .font(Constants.navigationTitleFont)
          .foregroundColor(Constants.navigationTitleTextColor)
          .padding(Constants.navigationTitlePadding)

        UserInfoView(
          accountInfo: $accountInfo,
          shouldApplySpacing: false,
          showLoginButton: Binding(get: { false }, set: { _ in }),
          showEditProfileButton: Binding(get: { false }, set: { _ in }),
          delegate: nil
        )
        .padding(.top, Constants.userInfoViewToPadding)

        HStack {
          Image(Constants.yralTokenImage)
            .resizable()
            .frame(width: Constants.tokenImageSize, height: Constants.tokenImageSize)
            .padding(.leading, Constants.yralHstackMargin)
            .padding(.vertical, Constants.yralHstackMargin)
          Text(Constants.yralTokenString)
            .font(Constants.tokenFont)
            .foregroundStyle(Constants.tokenTextColor)
            .padding(.vertical, Constants.yralHstackMargin)
          Spacer()
          Text(String(session.state.coins))
            .font(Constants.tokenValueFont)
            .foregroundStyle(Constants.tokenTextColor)
            .padding(.trailing, Constants.yralHstackMargin)
            .padding(.vertical, Constants.yralHstackMargin)
        }
        .frame(height: Constants.yralHstackHeight)
        .background(Constants.yralHstackBGColor)
        .cornerRadius(Constants.yralHstackCornerRadius)
        .overlay(
          RoundedRectangle(cornerRadius: Constants.yralHstackCornerRadius)
            .stroke(Constants.hstackBorderColor, lineWidth: .one)
        )

        HStack {
          Image(Constants.btcTokenImage)
            .resizable()
            .frame(width: Constants.tokenImageSize, height: Constants.tokenImageSize)
            .padding(.leading, Constants.yralHstackMargin)
            .padding(.vertical, Constants.btcHstackVerticalMargin)
          Text(Constants.btcTokenString)
            .font(Constants.tokenFont)
            .foregroundStyle(Constants.tokenTextColor)
            .padding(.vertical, Constants.btcHstackVerticalMargin)
          Spacer()
          Text(String(format: "%.8f", Double(btcToken)))
            .font(Constants.tokenValueFont)
            .foregroundStyle(Constants.tokenTextColor)
            .padding(.trailing, Constants.yralHstackMargin)
            .padding(.vertical, Constants.btcHstackVerticalMargin)
        }
        .frame(height: Constants.btcHstackHeight)
        .cornerRadius(Constants.yralHstackCornerRadius)
        .overlay(
          TopRoundedRectangle(cornerRadius: Constants.yralHstackCornerRadius)
            .stroke(Constants.hstackBorderColor, lineWidth: .one)
        )
        .background(TopRoundedRectangle(cornerRadius: Constants.yralHstackCornerRadius)
          .fill(Constants.yralHstackBGColor))
        if Locale.current.regionCode == Constants.indiaRegionCode {
          HStack(spacing: Constants.balanceHstackSpacing) {
            Spacer()
            Text(Constants.balanceString)
              .font(Constants.balanceFont)
              .foregroundStyle(Constants.balanceTextColor)
            Image(Constants.rupeeImage)
              .resizable()
              .frame(width: Constants.rupeeImageSize, height: Constants.rupeeImageSize)
            Text("\(Constants.currencySymbol)\(String(format: "%.0f", btcToken * btcToCurrencyValue))")
              .font(Constants.currencyFont)
              .foregroundColor(Constants.currencyTextColor)
              .padding(.vertical, Constants.currencyVerticalPadding)
              .padding(.horizontal, Constants.currencyHorizontalPadding)
              .background(Constants.currencyBGColor)
              .cornerRadius(Constants.currencyRadius)
              .padding(.trailing, Constants.yralHstackMargin)
          }
          .frame(height: Constants.currencyHStackHeight)
          .cornerRadius(Constants.yralHstackCornerRadius)
          .overlay(
            BottomRoundedRectangle(cornerRadius: Constants.yralHstackCornerRadius)
              .stroke(Constants.hstackBorderColor, lineWidth: .one)
          )
          .background(BottomRoundedRectangle(cornerRadius: Constants.yralHstackCornerRadius)
            .fill(Constants.currencyHstackBGColor))
          .padding(.top, -Constants.vStackSpacing)
        }
        HStack {
          Spacer()
          Text("\(Constants.conversionString)\(String(btcToCurrencyValue))")
            .foregroundStyle(Constants.conversionTextColor)
            .font(Constants.conversionFont)
        }
        .padding(.top, -Constants.conversionHstackTopPadding)

        if showEarnBTCButton {
          Text(Constants.ctaTitle)
            .font(Constants.ctaFont)
            .foregroundColor(Constants.ctaColor)
            .padding(.vertical, Constants.ctaVertical)
            .padding(.horizontal, Constants.ctaHorizontal)
            .background(Constants.ctaBackground)
            .clipShape(
              RoundedRectangle(cornerRadius: Constants.ctaCornerRadius)
            )
            .onTapGesture {
              showEarnBTCBottomSheet = true
              AnalyticsModuleKt.getAnalyticsManager().trackEvent(event: HowToEarnClickedEventData())
            }
        }
      }
      .padding(.horizontal, Constants.horizontalPadding)
      Spacer()
    }
    .onChange(of: viewModel.event) { event in
      switch event {
      case .accountInfoFetched(let info):
        accountInfo = info
      case .btcBalanceFetched(let balance):
        btcToken = balance
      case .exchangeRateFetched(let exchangeRate):
        btcToCurrencyValue = exchangeRate
      case .videoViewedRewardsStatusFetched(let status):
        showEarnBTCButton = status
        FirebaseLottieManager.shared.downloadAndSaveToCache(
          forPath: Constants.lottiePath,
          ignoreCache: true
        )
      default: break
      }
    }
    .onReceive(session.phasePublisher) { phase in
      switch phase {
      case .loggedOut, .ephemeral, .permanent:
        Task {
          await viewModel.fetchAccountInfo()
        }
      default: break
      }
    }
    .overlay(alignment: .center, content: {
      if showEarnBTCBottomSheet {
        Color.black.opacity(Constants.bottomSheetBackgroundOpacity)
          .ignoresSafeArea()
          .transition(.opacity)
      }
    })
    .fullScreenCover(isPresented: $showEarnBTCBottomSheet) {
      VideoViewedRewardsBottomSheet {
        showEarnBTCBottomSheet = false
      }
      .background( ClearBackgroundView() )
    }
    .task {
      Task {
        await viewModel.fetchBTCBalance()
      }
      guard isLoadingFirstTime else { return }
      await viewModel.fetchAccountInfo()
      isLoadingFirstTime = false
      await viewModel.fetchVideoViewedRewardsStatus()
      await viewModel.fetchExchangeRate()
    }
  }
}

extension WalletView {
  enum Constants {
    static let vStackSpacing: CGFloat = 24.0
    static let userInfoViewToPadding = -4.0
    static let horizontalPadding: CGFloat = 16.0
    static let yralHstackHeight = 65.0
    static let btcHstackHeight = 56.0
    static let yralHstackMargin = 8.0
    static let btcHstackVerticalMargin = 4.0
    static let yralHstackCornerRadius = 8.0
    static let tokenImageSize = 48.0
    static let navigationTitlePadding = EdgeInsets(
      top: 20.0,
      leading: 0.0,
      bottom: 16.0,
      trailing: 0.0
    )
    static let rupeeImageSize = 28.0
    static let balanceHstackSpacing = 8.0
    static let currencyVerticalPadding = 4.0
    static let currencyHorizontalPadding = 8.0
    static let currencyRadius = 8.0
    static let currencyHStackHeight = 44.0
    static let conversionHstackTopPadding = 12.0

    static let navigationTitle: String = "My Wallet"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let yralHstackBGColor = YralColor.grey800.swiftUIColor
    static let yralTokenImage = "yral_token_wallet"
    static let yralTokenString = "YRAL"
    static let tokenFont = YralFont.pt20.bold.swiftUIFont
    static let tokenValueFont = YralFont.pt24.bold.swiftUIFont
    static let tokenTextColor = YralColor.grey50.swiftUIColor
    static let hstackBorderColor = YralColor.grey700.swiftUIColor
    static let btcTokenImage = "btc_token_wallet"
    static let btcTokenString = "Bitcoin"
    static let balanceString = "INR Balance"
    static let balanceTextColor = YralColor.grey50.swiftUIColor
    static let balanceFont = YralFont.pt12.regular.swiftUIFont
    static let rupeeImage = "rupee_wallet"
    static let currencySymbol: String = "₹ "
    static let currencyTextColor = YralColor.grey50.swiftUIColor
    static let currencyFont = YralFont.pt14.semiBold.swiftUIFont
    static let currencyBGColor = YralColor.green400.swiftUIColor
    static let currencyHstackBGColor = YralColor.grey700.swiftUIColor
    static var conversionString: String {
      let locale = Locale.current
      let currencySymbol = locale.currencySymbol ?? "$"
      return "1 Bitcoin = \(currencySymbol)"
    }
    static let conversionTextColor = YralColor.grey500.swiftUIColor
    static let conversionFont = YralFont.pt12.regular.swiftUIFont
    static let indiaRegionCode = "IN"

    static let ctaTitle = "How to get Bitcoin"
    static let ctaFont = YralFont.pt14.bold.swiftUIFont
    static let ctaColor = YralColor.grey800.swiftUIColor
    static let ctaBackground = YralColor.grey50.swiftUIColor
    static let ctaCornerRadius = 8.0
    static let ctaVertical = 10.0
    static let ctaHorizontal = 20.0
    static let bottomSheetBackgroundOpacity = 0.8
    static let lottiePath = "btc_rewards/btc_rewards_views.json"
  }
}
