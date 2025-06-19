//
//  Color+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 17/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

extension Color {
  init(hex: String) {
    let hex = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

    var hexFormatted = hex
    if hexFormatted.hasPrefix("#") {
      hexFormatted.removeFirst()
    }

    assert(hexFormatted.count == 6, "Invalid hex code used.")

    var rgbValue: UInt64 = 0
    Scanner(string: hexFormatted).scanHexInt64(&rgbValue)

    let red = Double((rgbValue & 0xFF0000) >> 16) / 255
    let green = Double((rgbValue & 0x00FF00) >> 8) / 255
    let blue = Double(rgbValue & 0x0000FF) / 255

    self.init(red: red, green: green, blue: blue)
  }
}
