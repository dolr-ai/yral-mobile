//
//  UIApplication+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 30/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

extension UIApplication {
  var topSafeAreaInset: CGFloat {
    let window = connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first { $0.isKeyWindow }

    return window?.safeAreaInsets.top ?? 0
  }
}
