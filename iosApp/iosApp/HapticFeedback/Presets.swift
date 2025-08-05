//
//  Presets.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import UIKit

extension HapticFeedback {
  static public var selection: HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_SELECTION") {
      let generator = UISelectionFeedbackGenerator()
      generator.prepare()
      generator.selectionChanged()
    }
  }
}

extension HapticFeedback {
  static public var success: HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_SUCCESS") {
      let generator = UINotificationFeedbackGenerator()
      generator.prepare()
      generator.notificationOccurred(.success)
    }
  }

  static public var warning: HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_WARNING") {
      let generator = UINotificationFeedbackGenerator()
      generator.prepare()
      generator.notificationOccurred(.warning)
    }
  }

  static public var error: HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_ERROR") {
      let generator = UINotificationFeedbackGenerator()
      generator.prepare()
      generator.notificationOccurred(.error)

    }
  }
}

extension HapticFeedback {
  static public var impact: HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_IMPACT") {
      let generator = UIImpactFeedbackGenerator(style: .medium)
      generator.prepare()
      generator.impactOccurred()
    }
  }

  static public func impact(
    weight: HapticFeedback.Weight,
    intensity: Double = 1.0
  ) -> HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_IMPACT_\(weight.rawValue)_\(intensity)") {
      let style: UIImpactFeedbackGenerator.FeedbackStyle = switch weight {
      case .light: .light
      case .medium: .medium
      case .heavy: .heavy
      }
      let generator = UIImpactFeedbackGenerator(style: style)
      generator.prepare()
      generator.impactOccurred(intensity: intensity)
    }
  }

  static public func impact(
    flexibility: HapticFeedback.Flexibility,
    intensity: Double = 1.0
  ) -> HapticFeedback {
    HapticFeedback("SwiftUI_Haptics_TYPE_IMPACT_\(flexibility.rawValue)_\(intensity)") {
      let style: UIImpactFeedbackGenerator.FeedbackStyle = switch flexibility {
      case .rigid: .rigid
      case .solid: .medium
      case .soft: .soft
      }
      let generator = UIImpactFeedbackGenerator(style: style)
      generator.prepare()
      generator.impactOccurred(intensity: intensity)
    }
  }
}
