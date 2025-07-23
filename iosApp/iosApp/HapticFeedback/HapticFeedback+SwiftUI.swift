//
//  HapticFeedback+SwiftUI.swift
//  iosApp
//
//  Created by Samarth Paboowal on 23/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

extension View {
  @ViewBuilder public func hapticFeedback<T: Equatable>(
    _ feedback: HapticFeedback,
    trigger: T
  ) -> some View {
    if #available(iOS 17.0, *) {
      onChange(of: trigger) {
        feedback.perform()
      }
    } else {
      onChange(of: trigger) { _ in
        feedback.perform()
      }
    }
  }

  @ViewBuilder public func hapticFeedback<T: Equatable>(
    trigger: T,
    _ feedback: @escaping (T, T) -> HapticFeedback?
  ) -> some View {
    if #available(iOS 17.0, *) {
      onChange(of: trigger) { oldValue, newValue in
        feedback(oldValue, newValue)?.perform()
      }
    } else {
      onChange(of: trigger) { [oldValue = trigger] newValue in
        feedback(oldValue, newValue)?.perform()
      }
    }
  }

  @ViewBuilder public func hapticFeedback<T: Equatable>(
    _ feedback: HapticFeedback,
    trigger: T,
    condition: @escaping (T, T) -> Bool
  ) -> some View {
    if #available(iOS 17.0, macOS 14.0, watchOS 10.0, tvOS 17.0, macCatalyst 17.0, visionOS 1.0, *) {
      onChange(of: trigger) { oldValue, newValue in
        condition(oldValue, newValue) ? feedback.perform() : ()
      }
    } else {
      onChange(of: trigger) { [oldValue = trigger] newValue in
        condition(oldValue, newValue) ? feedback.perform() : ()
      }
    }
  }
}
