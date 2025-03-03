//
//  View+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

extension View {
  /// A modifier that shows a placeholder when `shouldShow` is true.
  func placeholder<Content: View>(
    when shouldShow: Bool,
    alignment: Alignment = .leading,
    @ViewBuilder placeholder: () -> Content
  ) -> some View {
    ZStack(alignment: alignment) {
      placeholder().opacity(shouldShow ? 1 : 0)
      self
    }
  }
}

extension UIApplication {
  func endEditing() {
    sendAction(#selector(UIResponder.resignFirstResponder),
               to: nil, from: nil, for: nil)
  }
}

extension View {
  /// A modifier that hides the keyboard when tapping anywhere outside text fields.
  func hideKeyboardOnTap() -> some View {
    self.onTapGesture {
      UIApplication.shared.endEditing()
    }
  }
}
