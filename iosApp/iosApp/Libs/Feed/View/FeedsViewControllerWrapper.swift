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
  @Binding var walletPhase: WalletPhase
  @Binding var walletOutcome: WalletPhase
  @EnvironmentObject var session: SessionManager

  func makeUIViewController(context: Context) -> UINavigationController {
    feedsViewController.onBackButtonTap = { [weak feedsViewController] in
      $showFeeds.wrappedValue = false
      feedsViewController?.onBackButtonTap = nil
    }
    let navigationController = UINavigationController(rootViewController: feedsViewController)
    navigationController.view.backgroundColor = .clear
    navigationController.edgesForExtendedLayout = .all
    feedsViewController.edgesForExtendedLayout = .all
    feedsViewController.extendedLayoutIncludesOpaqueBars = true
    return navigationController
  }

  func updateUIViewController(_ uiViewController: UINavigationController, context: Context) {
    if let feedVC = uiViewController.viewControllers.first as? FeedsViewController {
      feedVC.walletAnimationDelegate = context.coordinator
      feedVC.session = session
    }
  }

  func makeCoordinator() -> Coordinator { Coordinator(self) }
}

class Coordinator: NSObject, FeedsViewControllerRechargeDelegate {
  var parent: FeedsViewControllerWrapper
  init(_ parent: FeedsViewControllerWrapper) { self.parent = parent }

  func walletAnimationStarted() {
    withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
      self.parent.walletOutcome = .none
      self.parent.walletPhase = .loading
    }
  }
  func walletAnimationEnded(success: Bool, coins: Int64) {
    self.parent.walletOutcome = success ? .success(coins) : .failure
  }
}
