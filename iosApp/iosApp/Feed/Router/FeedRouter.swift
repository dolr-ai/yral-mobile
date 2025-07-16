//
//  FeedRouter.swift
//  iosApp
//
//  Created by Samarth Paboowal on 16/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol FeedRouterProtocol {
  func getAccountView() -> AccountView
}

final class FeedRouter: FeedRouterProtocol {
  weak var feedDI: FeedDIContainer!

  init(feedDI: FeedDIContainer) {
    self.feedDI = feedDI
  }

  func getAccountView() -> AccountView {
    feedDI.makeAccountView()
  }
}
