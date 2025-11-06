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
    corners: UIRectCorner? = nil,
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

    if let opacity = opacity {
      gradient.opacity = opacity
    }

    if let corners = corners, let radius = cornerRadius {
      let path = UIBezierPath(
        roundedRect: gradient.bounds,
        byRoundingCorners: corners,
        cornerRadii: CGSize(width: radius, height: radius)
      )
      let mask = CAShapeLayer()
      mask.path = path.cgPath
      gradient.mask = mask
    } else {
      gradient.mask = nil
      gradient.cornerRadius = cornerRadius ?? 0
    }
  }
}
