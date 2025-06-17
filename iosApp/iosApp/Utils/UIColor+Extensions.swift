//
//  UIColor+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 17/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

extension UIColor {
  convenience init(hex: String) {
    var hexFormatted = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

    if hexFormatted.hasPrefix("#") {
      hexFormatted.removeFirst()
    }

    assert(hexFormatted.count == 6, "Invalid hex code used.")

    var rgbValue: UInt64 = 0
    Scanner(string: hexFormatted).scanHexInt64(&rgbValue)

    let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255
    let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255
    let blue = CGFloat(rgbValue & 0x0000FF) / 255

    self.init(red: red, green: green, blue: blue, alpha: 1.0)
  }
}
