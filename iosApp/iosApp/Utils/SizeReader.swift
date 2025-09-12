//
//  SizeReader.swift
//  iosApp
//
//  Created by Samarth Paboowal on 11/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

private struct SizeKey: PreferenceKey {
  static var defaultValue: CGSize = .zero
  static func reduce(value: inout CGSize, nextValue: () -> CGSize) { value = nextValue() }
}

private struct ReadSize: ViewModifier {
  let onChange: (CGSize) -> Void
  func body(content: Content) -> some View {
    content
      .overlay(
        GeometryReader { proxy in
          Color.clear
            .preference(key: SizeKey.self, value: proxy.size)
        }
      )
      .onPreferenceChange(SizeKey.self, perform: onChange)
  }
}

extension View {
  func readSize(_ onChange: @escaping (CGSize) -> Void) -> some View {
    self.modifier(ReadSize(onChange: onChange))
  }
}
