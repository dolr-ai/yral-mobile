//
//  UIView+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 22/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

extension UIView {
  func setGradientBackground(
    colors: [UIColor],
    start: CGPoint = .init(x: 0, y: 0),
    end: CGPoint = .init(x: 0, y: 1),
    frame: CGRect? = nil,
    cornerRadius: CGFloat? = nil,
    opacity: Float? = nil
  ) {
    let gradient: CAGradientLayer
    if let existing = layer.sublayers?.first as? CAGradientLayer {
      gradient = existing
    } else {
      gradient = CAGradientLayer()
      layer.insertSublayer(gradient, at: .zero)
    }

    gradient.frame = frame == nil ? bounds : frame!
    gradient.colors = colors.map { $0.cgColor }
    gradient.startPoint = start
    gradient.endPoint   = end
    if let cornerRadius = cornerRadius {
      gradient.cornerRadius = cornerRadius
    }
    if let opacity = opacity {
      gradient.opacity = opacity
    }
  }
}
