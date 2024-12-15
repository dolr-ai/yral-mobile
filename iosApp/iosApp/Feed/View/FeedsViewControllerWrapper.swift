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
    return navigationController
  }

  func updateUIViewController(_ uiViewController: UINavigationController, context: Context) { }
}
