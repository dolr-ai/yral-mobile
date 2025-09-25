//
//  String+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 25/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

extension String {
  var currencySymbol: String? {
    switch self.uppercased() {
    case "INR":
      return "₹"
    case "USD":
      return "$"
    default:
      return nil
    }
  }
}
