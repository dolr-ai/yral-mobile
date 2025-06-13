//
//  RoundedCorner.swift
//  iosApp
//
//  Created by Samarth Paboowal on 24/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct RoundedCorner: Shape {
  var radius: CGFloat = .infinity
  var corners: UIRectCorner = .allCorners

  func path(in rect: CGRect) -> Path {
    let path = UIBezierPath(roundedRect: rect,
                            byRoundingCorners: corners,
                            cornerRadii: CGSize(width: radius, height: radius))
    return Path(path.cgPath)
  }
}
