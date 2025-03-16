//
//  AppTheme.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import SwiftUI

enum YralColor: String {
  case primary50
  case primary100
  case primary200
  case primary300
  case primary400
  case grey0
  case grey50
  case grey100
  case grey200
  case grey300
  case grey400
  case grey500
  case grey600
  case grey700
  case grey800
  case grey900
  case grey950
  case green50
  case green100
  case green200
  case green300
  case green400
  case green500
  case blue50
  case blue100
  case blue200
  case blue300
  case yellow50
  case yellow100
  case yellow200
  case yellow300
  case yellow400
  case red50
  case red100
  case red200
  case red300
  case red400
  case red500

  var uiColor: UIColor {
    UIColor(named: rawValue) ?? .black
  }

  var swiftUIColor: Color {
    Color(rawValue)
  }
}
