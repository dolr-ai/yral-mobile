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

  func displayAccountView() -> AccountView
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

  func displayAccountView() -> AccountView {
    return profileDI.makeAccount()
  }
}
