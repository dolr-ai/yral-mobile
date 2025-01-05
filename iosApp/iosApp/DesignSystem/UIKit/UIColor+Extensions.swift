//
//  UIColor+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 02/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

extension UIColor {
  static func hexColor(_ hex: String) -> UIColor {
    var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

    if cString.hasPrefix("#") {
      cString.remove(at: cString.startIndex)
    }

    if (cString.count != 6) && (cString.count != 8) {
      return .clear
    }

    var rgbValue: UInt64 = 0
    Scanner(string: cString).scanHexInt64(&rgbValue)

    let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
    let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
    let blue = CGFloat(rgbValue & 0x0000FF) / 255.0
    var alpha: CGFloat = 1.0

    if cString.count == 8 {
      alpha = CGFloat((rgbValue & 0xFF000000) >> 24) / 255.0
    }

    return UIColor(red: red, green: green, blue: blue, alpha: alpha)
  }
}
