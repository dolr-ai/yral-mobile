//
//  AppTheme.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import SwiftUI

enum YralFont {
  case pt8
  case pt10
  case pt12
  case pt14
  case pt16
  case pt18
  case pt20
  case pt24
  case pt32
  case pt40
  case pt64

  private var size: CGFloat {
    switch self {
    case .pt8:
      return 8
    case .pt10:
      return 10
    case .pt12:
      return 12
    case .pt14:
      return 14
    case .pt16:
      return 16
    case .pt18:
      return 18
    case .pt20:
      return 20
    case .pt24:
      return 24
    case .pt32:
      return 32
    case .pt40:
      return 40
    case .pt64:
      return 64
    }
  }

  var uiFont: UIFont {
    let fontName = "\(Constants.fontName)-\(Weight.regular)"
    return UIFont(name: fontName, size: size)
    ?? UIFont.systemFont(ofSize: size, weight: .regular)
  }

  var swiftUIFont: Font {
    Font.custom("\(Constants.fontName)-\(Weight.regular)", size: size)
  }

  enum Weight: String {
    case regular = "Regular"
    case medium = "Medium"
    case semiBold = "SemiBold"
    case bold = "Bold"

    var systemWeight: UIFont.Weight {
      switch self {
      case .regular: return .regular
      case .medium: return .medium
      case .semiBold: return .semibold
      case .bold: return .bold
      }
    }
  }

  func withWeight(_ weight: Weight) -> (uiFont: UIFont, swiftUIFont: Font) {
    let fontName = "\(Constants.fontName)-\(weight.rawValue)"
    let customUIFont = UIFont(name: fontName, size: size)
    ?? UIFont.systemFont(ofSize: size, weight: weight.systemWeight)
    let customSwiftUIFont = Font.custom(fontName, size: size)
    return (customUIFont, customSwiftUIFont)
  }

  var regular: (uiFont: UIFont, swiftUIFont: Font) {
    withWeight(.regular)
  }

  var medium: (uiFont: UIFont, swiftUIFont: Font) {
    withWeight(.medium)
  }

  var semiBold: (uiFont: UIFont, swiftUIFont: Font) {
    withWeight(.semiBold)
  }

  var bold: (uiFont: UIFont, swiftUIFont: Font) {
    withWeight(.bold)
  }
}

extension YralFont {
  enum Constants {
    static let fontName = "KumbhSans"
  }
}
