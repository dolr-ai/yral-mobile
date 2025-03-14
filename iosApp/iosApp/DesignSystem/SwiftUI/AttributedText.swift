//
//  AttributedText.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 12/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import SwiftUI

struct AttributedText: UIViewRepresentable {
  let attributedString: NSAttributedString
  var horizontalPadding: CGFloat = .zero
  var alignment: NSTextAlignment = .center

  func makeUIView(context: Context) -> UILabel {
    let label = UILabel()
    label.numberOfLines = .zero
    label.lineBreakMode = .byWordWrapping
    label.textAlignment = alignment
    return label
  }

  func updateUIView(_ uiView: UILabel, context: Context) {
    uiView.attributedText = attributedString
    let maxWidth = UIScreen.main.bounds.width - .two * horizontalPadding
    uiView.preferredMaxLayoutWidth = maxWidth
    uiView.textAlignment = alignment
  }
}
