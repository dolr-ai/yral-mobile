//
//  SwiftUI+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 23/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

extension Binding where Value == Bool {
  var inverted: Binding<Bool> {
    Binding<Bool>(
      get: { !self.wrappedValue },
      set: { self.wrappedValue = !$0 }
    )
  }
}
