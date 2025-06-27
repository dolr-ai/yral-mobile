//
//  ToastManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

final class ToastManager {
  static func showToast(
    type: ToastType,
    onRetry: (() -> Void)? = nil,
    onTap: (() -> Void)? = nil
  ) {
    guard let windowScene = UIApplication.shared.connectedScenes
      .compactMap({ $0 as? UIWindowScene })
      .first(where: { $0.activationState == .foregroundActive }),
          let window = windowScene.windows.first(where: { $0.isKeyWindow }) else {
      return
    }

    let safeAreaInsets = window.safeAreaInsets
    let toastWidth = window.bounds.width - (Constants.toastWidthDelta + safeAreaInsets.left + safeAreaInsets.right)
    let toastX = safeAreaInsets.left + (Constants.toastWidthDelta / 2)
    let toastView = ToastView(type: type, buttonAction: onRetry, tapAction: onTap)

    toastView.frame = CGRect(
      x: toastX,
      y: Constants.toastInitialY,
      width: toastWidth,
      height: .zero
    )
    window.addSubview(toastView)
    toastView.layoutIfNeeded()

    let targetSize = CGSize(width: toastWidth, height: UIView.layoutFittingCompressedSize.height)
    let finalHeight = toastView.systemLayoutSizeFitting(targetSize).height
    toastView.frame = CGRect(
      x: toastX,
      y: -finalHeight,
      width: toastWidth,
      height: finalHeight
    )

    UIView.animate(withDuration: CGFloat.animationPeriod) {
      toastView.frame.origin.y = safeAreaInsets.top
    }
  }
}

extension ToastManager {
  enum Constants {
    static let toastInitialY: CGFloat = -1000
    static let toastWidthDelta: CGFloat = 32
  }
}
