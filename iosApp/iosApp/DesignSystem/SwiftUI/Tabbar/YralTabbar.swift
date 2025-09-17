//
//  YralTabbar.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import SwiftUI

// swiftlint: disable all
final class YralTabBar: UITabBar {
  let extraHeight: CGFloat
  init(extraHeight: CGFloat = 20) {
    self.extraHeight = extraHeight
    super.init(frame: .zero)
  }
  required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

  override func sizeThatFits(_ size: CGSize) -> CGSize {
    var newSize = super.sizeThatFits(size)
    newSize.height += extraHeight
    return newSize
  }
}

// ✅ NEW: UIView-based injector that lives in the same responder chain as TabView
final class TabBarInjectorView: UIView {
  var extraHeight: CGFloat = 20
  var onHeightChange: ((CGFloat) -> Void)?

  override func didMoveToWindow() {
    super.didMoveToWindow()
    // Run on next runloop so TabView hierarchy is fully mounted
    DispatchQueue.main.async { [weak self] in self?.installIfNeeded() }
  }

  override func didMoveToSuperview() {
    super.didMoveToSuperview()
    DispatchQueue.main.async { [weak self] in self?.installIfNeeded() }
  }

  func installIfNeeded() {
    guard let tc = enclosingTabBarController() else {
      // Try again shortly if we mounted before TabView finished building
      DispatchQueue.main.async { [weak self] in self?.installIfNeeded() }
      return
    }

    if !(tc.tabBar is YralTabBar) {
      let newBar = YralTabBar(extraHeight: extraHeight)
      tc.setValue(newBar, forKey: "tabBar")                 // ✅ swap the real bar
      tc.additionalSafeAreaInsets.bottom = -extraHeight     // ✅ neutralize safe-area bump
      tc.tabBar.isTranslucent = false
      if #available(iOS 15.0, *) {
        tc.tabBar.scrollEdgeAppearance = tc.tabBar.standardAppearance
      }
      tc.view.setNeedsLayout()
      tc.view.layoutIfNeeded()
    }

    onHeightChange?(tc.tabBar.frame.height)
  }

  private func enclosingTabBarController() -> UITabBarController? {
    // Walk the responder chain: UIView -> UIHostingController (tab child) -> UITabBarController
    var responder: UIResponder? = self
    while let r = responder {
      if let vc = r as? UIViewController {
        if let tc = vc.tabBarController {
          return tc
        }
        if let tc = vc as? UITabBarController {
          return tc
        }
      }
      responder = r.next
    }
    return nil
  }
}

struct TabBarInjector: UIViewRepresentable {
  let extraHeight: CGFloat
  var onHeightChange: ((CGFloat) -> Void)?

  func makeUIView(context: Context) -> TabBarInjectorView {
    let v = TabBarInjectorView()
    v.isUserInteractionEnabled = false
    v.backgroundColor = .clear
    v.extraHeight = extraHeight
    v.onHeightChange = onHeightChange
    return v
  }

  func updateUIView(_ uiView: TabBarInjectorView, context: Context) {
    uiView.extraHeight = extraHeight        // ✅ allow live tweaks
    uiView.onHeightChange = onHeightChange
    // Try to install in case the hierarchy is now available
    DispatchQueue.main.async { [weak uiView] in uiView?.installIfNeeded() }
  }
}
// swiftlint: enable all