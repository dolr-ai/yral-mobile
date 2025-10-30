//
//  ProfileRouter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import SwiftUI

protocol ProfileRouterProtocol {
  func displayUserVideoFeed(
    existingFeeds: [FeedResult],
    info: MyVideosFeedInfo,
    showFeeds: Binding<Bool>,
    walletPhase: Binding<WalletPhase>,
    walletOutcome: Binding<WalletPhase>
  ) -> FeedsViewControllerWrapper

  func displayAccountView(showAccount: Binding<Bool>) -> AccountView

  func displayEditProfileView(
    showEditProfile: Binding<Bool>,
    accountInfo: Binding<AccountInfo>
  ) -> EditProfileView

  func displayVideoInsightsBottomSheet(
    openedFromFeed: Bool,
    videoInfo: ProfileVideoInfo,
    onComplete: @escaping (Int64?) -> Void
  ) -> VideoInsightsBottomSheet
}

final class ProfileRouter: ProfileRouterProtocol {
  weak var profileDI: ProfileDIContainer!

  init(profileDI: ProfileDIContainer) {
    self.profileDI = profileDI
  }

  func displayUserVideoFeed(
    existingFeeds: [FeedResult],
    info: MyVideosFeedInfo,
    showFeeds: Binding<Bool>,
    walletPhase: Binding<WalletPhase>,
    walletOutcome: Binding<WalletPhase>
  ) -> FeedsViewControllerWrapper {
    return profileDI.makeMyVideosView(
      existingFeeds: existingFeeds,
      info: info,
      showFeeds: showFeeds,
      walletPhase: walletPhase,
      walletOutcome: walletOutcome
    )
  }

  func displayAccountView(showAccount: Binding<Bool>) -> AccountView {
    return profileDI.makeAccount(showAccount: showAccount)
  }

  func displayEditProfileView(showEditProfile: Binding<Bool>, accountInfo: Binding<AccountInfo>) -> EditProfileView {
    return profileDI.makeEditProfile(
      showEditProfile: showEditProfile,
      accountInfo: accountInfo
    )
  }

  func displayVideoInsightsBottomSheet(
    openedFromFeed: Bool,
    videoInfo: ProfileVideoInfo,
    onComplete: @escaping (Int64?) -> Void
  ) -> VideoInsightsBottomSheet {
    return profileDI.makeVideoInsightsBottomSheet(
      openedFromFeed: openedFromFeed,
      videoInfo: videoInfo,
      onComplete: onComplete
    )
  }
}
