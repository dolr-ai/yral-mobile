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
    guard !isWalletLoading else { return }
    isWalletLoading = true
    pendingRechargeResult = nil
    lottieView.alpha = .zero
    lottieView.isHidden = false
    lottieView.backgroundColor = .black.withAlphaComponent(Constants.lottieBGOpacity)
    lottieView.stop()
    lottieView.animation = .named(Constants.rechargeLoadingLottie)
    lottieView.loopMode = .loop
    lottieView.play()
    UIView.animate(withDuration: CGFloat.animationPeriod) {
      self.lottieView.alpha = .one
    }
    delegate?.walletAnimationStarted()
  }

  func applyRechargeResult(_ result: RechargeResult, coins: Int64) {
    guard isWalletLoading else { play(result, coins: coins); return }
    pendingRechargeResult = result
    isWalletLoading = false
    lottieView.loopMode = .playOnce
    lottieView.play { [weak self] _ in
      guard let self = self else { return }
      if let pending = self.pendingRechargeResult {
        self.pendingRechargeResult = nil
        self.play(pending, coins: coins)
      }
    }
  }

  private func play(_ result: RechargeResult, coins: Int64) {
    switch result {
    case .success: showRechargeSuccessAnimation(coins: coins)
    case .failure: showRechargeFailureAnimation()
    }
  }

  func showRechargeSuccessAnimation(coins: Int64) {
    playAndDismiss(animationName: Constants.rechargeSuccessLottie, coins: coins)
  }

  func showRechargeFailureAnimation() {
    playAndDismiss(animationName: Constants.rechargeFailureLottie, coins: .zero)
  }

  private func playAndDismiss(animationName: String, coins: Int64) {
    lottieView.animation = .named(animationName)
    lottieView.loopMode = .playOnce
    lottieView.backgroundColor = .black.withAlphaComponent(Constants.lottieBGOpacity)
    lottieView.isHidden = false
    lottieView.alpha = .one

    lottieView.play { [weak self] finished in
      guard finished, let self = self else { return }

      UIView.animate(
        withDuration: animationName == Constants.rechargeSuccessLottie ?
        CGFloat.animationPeriod : CGFloat.one
      ) { self.lottieView.alpha = .zero } completion: { _ in
        self.lottieView.isHidden = true
        self.lottieView.backgroundColor = .clear
        self.lottieView.animation = nil
        self.lottieView.alpha = .one
        self.delegate?.walletAnimationEnded(
          success: animationName == Constants.rechargeSuccessLottie
          , coins: coins
        )
        self.initialStatePublisher.send(
          SmileyGame(
            config: SmileyGameConfig.shared.config,
            state: self.smileyGame?.state ?? .notPlayed
          )
        )
      }
    }
  }
}
