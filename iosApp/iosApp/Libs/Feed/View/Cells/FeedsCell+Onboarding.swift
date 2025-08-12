//
//  FeedsCell+Onboarding.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

extension FeedsCell {
  func showOnboardingFlow() {
    guard let smileyView = smileyGameHostController?.view else { return }
    addOnboardingOverlayView()
    addOnboardingInfoView(smileyView)

    smileyView.layer.masksToBounds = false
    smileyView.layer.cornerRadius = Constants.smileyGameHeight / CGFloat.two
    smileyView.layer.shadowColor = Constants.smileyGameShadowColor
    smileyView.layer.shadowOffset = .zero
    smileyView.layer.shadowRadius = Constants.smileyShadowMaxRadius
    smileyView.layer.shadowOpacity = 0
    smileyView.layoutIfNeeded()
    smileyView.backgroundColor = .black
    smileyView.layer.shadowPath = UIBezierPath(
      roundedRect: smileyView.bounds,
      cornerRadius: smileyView.layer.cornerRadius
    ).cgPath

    let radiusAnimation = CABasicAnimation(keyPath: "shadowOpacity")
    radiusAnimation.fromValue = CGFloat.zero
    radiusAnimation.toValue = CGFloat.one
    radiusAnimation.duration = CGFloat.half
    radiusAnimation.autoreverses = true
    radiusAnimation.repeatCount = Float(CGFloat.three / CGFloat.animationPeriod)

    smileyView.layer.add(radiusAnimation, forKey: "smileyShadowPulse")

    onboardingOverlayView.alpha = .zero

    self.animatePublisher.send(SmileyGameConfig.shared.config)

    UIView.animate(withDuration: CGFloat.animationPeriod) { [weak self] in
      guard let self = self else { return }
      self.onboardingOverlayView.alpha = .one
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.five) {
      self.cleanupOnOnboardingCompletion(smileyView: smileyView)
      UserDefaultsManager.shared.set(true, for: .onboardingCompleted)
    }
  }

  private func addOnboardingOverlayView() {
    onboardingOverlayView.backgroundColor = Constants.onboardingBGColor
    contentView.addSubview(onboardingOverlayView)
    guard let smileyView = smileyGameHostController?.view else { return }
    contentView.bringSubviewToFront(smileyView)
    NSLayoutConstraint.activate([
      onboardingOverlayView.leadingAnchor.constraint(equalTo: leadingAnchor),
      onboardingOverlayView.trailingAnchor.constraint(equalTo: trailingAnchor),
      onboardingOverlayView.topAnchor.constraint(equalTo: topAnchor),
      onboardingOverlayView.bottomAnchor.constraint(equalTo: bottomAnchor)
    ])
  }

  private func addOnboardingInfoView(_ smileyView: UIView) {
    addSubview(onboardingInfoView)
    NSLayoutConstraint.activate([
      onboardingInfoView.leadingAnchor.constraint(
        equalTo: leadingAnchor,
        constant: Constants.onbaordingInfoViewHorizontalSpacing
      ),
      onboardingInfoView.trailingAnchor.constraint(
        equalTo: trailingAnchor,
        constant: -Constants.onbaordingInfoViewHorizontalSpacing
      ),
      onboardingInfoView.bottomAnchor.constraint(
        equalTo: smileyView.topAnchor,
        constant: Constants.onbaordingInfoViewBottom
      ),
      onboardingInfoView.heightAnchor.constraint(equalToConstant: Constants.onbaordingInfoViewHeight)
    ])

    let translateAnimation = CABasicAnimation(keyPath: "transform.translation.y")
    translateAnimation.fromValue = -Constants.onboardingInfoAnimationTranslation
    translateAnimation.toValue =  Constants.onboardingInfoAnimationTranslation
    translateAnimation.duration = CGFloat.half
    translateAnimation.autoreverses = true
    translateAnimation.repeatCount = .greatestFiniteMagnitude
    translateAnimation.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

    onboardingInfoView.layer.add(translateAnimation, forKey: "onboardingBob")
  }

  func cleanupOnOnboardingCompletion(smileyView: UIView) {
    self.onboardingOverlayView.removeFromSuperview()
    smileyView.layer.shadowOpacity = .zero
    smileyView.layer.removeAnimation(forKey: "smileyShadowPulse")
    self.onboardingInfoView.layer.removeAnimation(forKey: "onboardingBob")
    smileyView.backgroundColor = .clear
    self.onboardingInfoView.removeFromSuperview()
    self.animationCompletionPublisher.send()
  }
}
