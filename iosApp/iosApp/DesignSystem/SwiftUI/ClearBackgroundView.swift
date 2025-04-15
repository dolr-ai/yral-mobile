//
//  ClearBackgroundView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct ClearBackgroundView: UIViewRepresentable {
  func makeUIView(context: Context) -> UIView {
    return InnerView()
  }

  func updateUIView(_ uiView: UIView, context: Context) {
  }

  private class InnerView: UIView {
    override func didMoveToWindow() {
      super.didMoveToWindow()
      superview?.superview?.backgroundColor = .clear
    }
  }
}
