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

  func makeUIViewController(context: Context) -> UINavigationController {
    let navigationController = UINavigationController(rootViewController: feedsViewController)
<<<<<<< HEAD
    navigationController.navigationBar.isHidden = true
    feedsViewController.edgesForExtendedLayout = .all
    feedsViewController.extendedLayoutIncludesOpaqueBars = true
=======
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
    return navigationController
  }

  func updateUIViewController(_ uiViewController: UINavigationController, context: Context) { }
}
