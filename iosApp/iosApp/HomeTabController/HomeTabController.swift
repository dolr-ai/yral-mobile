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
  let profileView: ProfileView
  let uploadView: UploadView
  @State private var selectedTab: Int = .zero
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height

  init(
    feedsViewControllerWrapper: FeedsViewControllerWrapper,
    profileView: ProfileView,
    uploadView: UploadView
  ) {
    self.feedsViewControllerWrapper = feedsViewControllerWrapper
    self.profileView = profileView
    self.uploadView = uploadView
    UITabBar.appearance().backgroundColor = .black
    UITabBar.appearance().barTintColor = .black
    UITabBar.appearance().isTranslucent = false
  }

  var body: some View {
    VStack(spacing: .zero) {
      TabView(selection: $selectedTab) {
        feedsViewControllerWrapper
          .tabItem {
            Image(ImageResource(name: Constants.homeIconImageName, bundle: .main))
              .renderingMode(.original)
          }
          .ignoresSafeArea()
          .tag(Int.zero)
        uploadView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            Image(ImageResource(name: Constants.uploadIconImageName, bundle: .main)).renderingMode(.original)
              .ignoresSafeArea()
              .tag(Int.one)
          }
          .tag(Int.one)
        profileView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            Image(ImageResource(name: Constants.profileIconImageName, bundle: .main)).renderingMode(.original)
              .ignoresSafeArea()
              .tag(Int.one)
          }
          .tag(Int.two)
      }
      GeometryReader { geometry in
        let tabWidth = geometry.size.width/CGFloat.three
        let indicatorXPosition = CGFloat(selectedTab) * tabWidth + (tabWidth - Constants.indicatorWidth) / CGFloat.two
        HStack(spacing: .zero) {
          Spacer().frame(width: indicatorXPosition)
          Rectangle()
            .fill(Color(ColorResource(name: Constants.indicatorColorName, bundle: .main)))
            .frame(width: Constants.indicatorWidth, height: Constants.tabIndicatorHeight)
            .edgesIgnoringSafeArea(.top)
            .cornerRadius(Constants.tabIndicatorHeight/CGFloat.two)
            .offset(x: .zero, y: -self.tabBarHeight)
            .animation(.easeInOut, value: selectedTab)
          Spacer()
        }
      }
      .frame(height: .zero)
    }
    .background(Color.black.edgesIgnoringSafeArea(.all))
  }
}

#Preview {
  let feedsDIContainer = AppDIContainer().makeFeedDIContainer()
  let profileDIContainer = AppDIContainer().makeProfileDIContainer()
  HomeTabController(
    feedsViewControllerWrapper: feedsDIContainer .makeFeedsViewControllerWrapper(),
    profileView: profileDIContainer.makeProfileView(),
    uploadView: UploadView()
  )
}

extension HomeTabController {
  enum Constants {
    static let homeIconImageName = "home_tab"
    static let profileIconImageName = "profile_tab"
    static let uploadIconImageName = "upload_tab"
    static let tabIndicatorHeight: CGFloat = 2.0
    static let indicatorWidth = 30.0
    static let indicatorColorName = "tabIndicatorColor"
  }
}
