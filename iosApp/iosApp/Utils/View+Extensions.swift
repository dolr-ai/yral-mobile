//
//  View+Extensions.swift
//  iosApp
//
//  Created by Samarth Paboowal on 12/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

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
