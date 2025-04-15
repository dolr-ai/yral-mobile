//
//  UILabel+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

extension UILabel {
  func heightForWidth(_ width: CGFloat, maxLines: Int = 0) -> CGFloat {
    guard let text = self.text, !text.isEmpty else { return 0 }
    let measuringLabel = UILabel()
    measuringLabel.numberOfLines = maxLines == 0 ? 0 : maxLines
    measuringLabel.font = self.font
    measuringLabel.lineBreakMode = .byWordWrapping
    measuringLabel.text = text

    let size = measuringLabel.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
    return size.height
  }
}
