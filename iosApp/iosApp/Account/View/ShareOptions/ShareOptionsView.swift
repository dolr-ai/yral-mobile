//
//  ShareOptionsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI
import iosSharedUmbrella

struct ShareOptionsView: View {
  @Environment(\.openURL) private var openURL

  var body: some View {
    VStack(spacing: Constants.vStackSpacing) {
      Text(Constants.titleText)
        .font(Constants.titleFont)
        .foregroundColor(Constants.titleColor)

      HStack(spacing: Constants.hStackSpacing) {
        ForEach(Constants.socialMediaPlatforms, id: \.self) { platform in
          // swiftlint: disable multiple_closures_with_trailing_closure
          Button(action: {
            openURL(platform.url)
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: MenuClickedEventData(ctaType: .followOn)
            )
          }) {
            Image(platform.imageName)
              .frame(width: Constants.iconSize, height: Constants.iconSize)
              .background(Constants.iconBackground)
              .clipShape(Circle())
          }
          // swiftlint: enable multiple_closures_with_trailing_closure
        }
      }
    }
    .padding(.horizontal, Constants.horizontalPadding)
  }

  struct SocialMediaPlatform: Hashable {
    let imageName: String
    let url: URL
  }
}

extension ShareOptionsView {
  enum Constants {
    static let vStackSpacing: CGFloat = 24
    static let hStackSpacing: CGFloat = 16
    static let iconSize: CGFloat = 45
    static let horizontalPadding: CGFloat = 45
    static let titleText = "Follow us on"
    static let titleFont = YralFont.pt12.medium.swiftUIFont
    static let titleColor = YralColor.grey500.swiftUIColor
    static let iconBackground = YralColor.grey800.swiftUIColor

    static let socialMediaPlatforms = [
      SocialMediaPlatform(
        imageName: "telegram_icon",
        url: URL(string: AccountDetailsUtility.shared.getDetails()?.telegram ?? "https://t.me/+c-LTX0Cp-ENmMzI1")!
      ),
      SocialMediaPlatform(
        imageName: "discord_icon",
        url: URL(string: AccountDetailsUtility.shared.getDetails()?.discord ?? "https://discord.com/invite/GZ9QemnZuj")!
      ),
      SocialMediaPlatform(
        imageName: "x_icon",
        url: URL(string: AccountDetailsUtility.shared.getDetails()?.twitter ?? "https://twitter.com/Yral_app")!
      )
    ]
  }
}
