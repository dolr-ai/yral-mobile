//
//  View+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

extension View {
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
  func hideKeyboardOnTap() -> some View {
    self.onTapGesture {
      UIApplication.shared.endEditing()
    }
  }
}

extension View {
  @ViewBuilder
  func scrollLock(_ disabled: Bool) -> some View {
    if #available(iOS 16.0, *) {
      self.scrollDisabled(disabled)
    } else {
      self.allowsHitTesting(!disabled)
    }
  }
}
