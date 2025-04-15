//
//  FeedsViewControllerWrapper.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import SwiftUI

struct FeedsViewControllerWrapper: UIViewControllerRepresentable {
  let feedsViewController: FeedsViewController
  @Binding var showFeeds: Bool

  func makeUIViewController(context: Context) -> UINavigationController {
    feedsViewController.onBackButtonTap = {
      self.showFeeds = false
    }
    let navigationController = UINavigationController(rootViewController: feedsViewController)
    navigationController.view.backgroundColor = .clear
    navigationController.edgesForExtendedLayout = .all
    feedsViewController.edgesForExtendedLayout = .all
    feedsViewController.extendedLayoutIncludesOpaqueBars = true
    return navigationController
  }

  func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {  }
}
