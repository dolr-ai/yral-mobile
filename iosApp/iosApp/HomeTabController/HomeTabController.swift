//
//  HomeTabController.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct HomeTabController: View {
  let feedsViewControllerWrapper: FeedsViewControllerWrapper
  @State private var selectedTab: Int = .zero
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height

  init(feedsViewControllerWrapper: FeedsViewControllerWrapper) {
    self.feedsViewControllerWrapper = feedsViewControllerWrapper
    UITabBar.appearance().backgroundColor = .black
    UITabBar.appearance().barTintColor = .black
    UITabBar.appearance().isTranslucent = false
  }

  var body: some View {
    VStack(spacing: .zero) {
      TabView(selection: $selectedTab) {
        feedsViewControllerWrapper
          .tabItem {
            Image(
              uiImage: UIImage(
                named: Constants.homeIconImageName)?
                .withRenderingMode(.alwaysOriginal) ?? UIImage()).renderingMode(.original)
          }
          .ignoresSafeArea()
      }
      Rectangle()
        .fill(Color.pink)
        .frame(width: Constants.indicatorWidth, height: Constants.tabIndicatorHeight)
        .edgesIgnoringSafeArea(.top)
        .cornerRadius(Constants.tabIndicatorHeight/2)
        .offset(x: .zero, y: -self.tabBarHeight)
    }
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

#Preview {
  let feedsDIContainer = AppDIContainer().makeFeedDIContainer()
  HomeTabController(feedsViewControllerWrapper: feedsDIContainer.makeFeedsViewControllerWrapper())
}

extension HomeTabController {
  enum Constants {
    static let homeIconImageName = "home_tab"
    static let tabIndicatorHeight: CGFloat = 2.0
    static let indicatorWidth = 30.0
  }
}
