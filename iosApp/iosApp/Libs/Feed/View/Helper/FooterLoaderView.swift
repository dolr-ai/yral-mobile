//
//  FooterLoaderView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

final class FooterLoaderView: UICollectionReusableView, ReusableView {

  private let activityIndicator: LottieLoaderContainerView = {
    let loader = LottieLoaderContainerView(animationName: FeedsViewController.Constants.loaderLottie)
    loader.translatesAutoresizingMaskIntoConstraints = false
    return loader
  }()

  override init(frame: CGRect) {
    super.init(frame: frame)
    addSubview(activityIndicator)
    NSLayoutConstraint.activate([
      activityIndicator.centerXAnchor.constraint(equalTo: centerXAnchor),
      activityIndicator.centerYAnchor.constraint(equalTo: centerYAnchor),
      activityIndicator.widthAnchor.constraint(equalToConstant: FeedsViewController.Constants.indicatorSize),
      activityIndicator.heightAnchor.constraint(equalToConstant: FeedsViewController.Constants.indicatorSize)
    ])
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func startAnimating() {
    activityIndicator.startAnimating(in: self)
  }

  func stopAnimating() {
    activityIndicator.stopAnimating()
  }
}
