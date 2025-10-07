//
//  CGFloat+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

extension CGFloat {
  static let zero: CGFloat = 0
  static let pointOne: CGFloat = 0.1
  static let animationPeriod: CGFloat = 0.3
  static let half: CGFloat = 0.5
  static let pointSevenFive: CGFloat = 0.5
  static let one: CGFloat = 1
  static let two: CGFloat = 2
  static let three: CGFloat = 3
  static let four: CGFloat = 4
  static let five: CGFloat = 5
  static let thirteen: CGFloat = 13
  static let hundred: CGFloat = 100.0
  static let thousand: CGFloat = 1000.0
  static let apiDelay: CGFloat = 0.2
}

extension Double {
  static let one: Double = 1

  func rounded(toPlaces places: Int) -> Double {
    let divisor = pow(10.0, Double(places))
    return (self * divisor).rounded() / divisor
  }

  var cleanValue: String {
    return truncatingRemainder(dividingBy: 1) == 0 ? String(format: "%.0f", self) : String(format: "%.1f", self)
  }
}

extension Int {
  static let zero: Int = 0
  static let one: Int = 1
  static let two: Int = 2
  static let three: Int = 3
  static let four: Int = 4
  static let five: Int = 5
  static let ten: Int = 10
  static let thousand: Int = 1000

  var formattedWithSuffix: String {
    let num = Double(self)

    switch num {
    case 0..<1_000:
      return "\(self)"

    case 1_000..<1_000_000:
      let value = (num / 1_000).rounded(toPlaces: 1)
      return "\(value.cleanValue)K"

    case 1_000_000..<1_000_000_000:
      let value = (num / 1_000_000).rounded(toPlaces: 1)
      return "\(value.cleanValue)M"

    case 1_000_000_000..<1_000_000_000_000:
      let value = (num / 1_000_000_000).rounded(toPlaces: 1)
      return "\(value.cleanValue)B"

    default:
      let value = (num / 1_000_000_000_000).rounded(toPlaces: 1)
      return "\(value.cleanValue)T"
    }
  }
}
