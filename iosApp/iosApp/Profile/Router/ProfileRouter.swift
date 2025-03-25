//
//  ProfileRouter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ProfileRouterProtocol {
  func displayUserVideoFeed(existingFeeds: [FeedResult], info: MyVideosFeedInfo) -> FeedsViewControllerWrapper
}

final class ProfileRouter: ProfileRouterProtocol {
  weak var profileDI: ProfileDIContainer!

  init(profileDI: ProfileDIContainer) {
    self.profileDI = profileDI
  }

  func displayUserVideoFeed(existingFeeds: [FeedResult], info: MyVideosFeedInfo) -> FeedsViewControllerWrapper {
    return profileDI.makeMyVideosView(existingFeeds: existingFeeds, info: info)
  }
}
