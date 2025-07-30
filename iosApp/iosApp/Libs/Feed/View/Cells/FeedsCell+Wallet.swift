//
//  FeedsCell+Animations.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 29/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import Lottie

extension FeedsCell {
  func beginWalletRechargeLoading() {
    delegate?.walletAnimationStarted()
  }

  func applyRechargeResult(_ result: RechargeResult, coins: Int64) {
    self.initialStatePublisher.send(
      SmileyGame(
        config: SmileyGameConfig.shared.config,
        state: self.smileyGame?.state ?? .notPlayed
      )
    )
    delegate?.walletAnimationEnded(
      success: result == .success,
      coins: coins
    )
  }
}
