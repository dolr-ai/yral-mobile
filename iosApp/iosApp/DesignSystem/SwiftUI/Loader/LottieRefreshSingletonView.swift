//
//  LottieRefreshSingletonView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 31/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import SwiftUI

class LottieRefreshSingletonView: UIView {
  static let shared: UIView = {
    return LottieRefreshSingletonView.sharedInstance(size: CGSize(width: Constants.size, height: Constants.size))
  }()

  private class func sharedInstance(size: CGSize) -> UIView {
    let pointX = (UIScreen.main.bounds.width - size.width) / .two
    let view = LottieRefreshSingletonView(frame: CGRect(x: pointX, y: .zero, width: size.width, height: size.height))
    let loaderView = LottieLoaderView(animationName: Constants.lottieName)

    let hostingController = UIHostingController(rootView: loaderView)
    hostingController.view.backgroundColor = .clear
    hostingController.view.frame = view.bounds
    hostingController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    view.addSubview(hostingController.view)
    return view
  }
}

extension LottieRefreshSingletonView {
  enum Constants {
    static let lottieName = "Yral_Loader"
    static let size = 24.0
  }
}
