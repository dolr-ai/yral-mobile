//
//  FeedsCell+PlayToScroll.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

extension FeedsCell {
  func showPlayToScroll() {
    guard let smileyView = smileyGameHostController?.view,
          case .notPlayed = smileyGame?.state,
          smileyView.isDescendant(of: contentView) else { return }
    let gen = { overlayGeneration &+= 1; return overlayGeneration }()
    addGameInfoOverlayView()
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissPlayToScrollOnTap))
    gameInfoOverlayView.addGestureRecognizer(tapGesture)
    animateGameInfoOverlayView(smileyView: smileyView) { [weak self] in
      guard let self = self,
            self.overlayGeneration == gen,
            smileyView.isDescendant(of: contentView),
            smileyView.window != nil else { return }
      self.addPlayToScrollInfoView(smileyView)
      DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.five) { [weak self] in
        guard let self = self,
              self.overlayGeneration == gen,
              smileyView.isDescendant(of: self.contentView) else { return }
        self.cleanupOnOnboardingCompletion(smileyView: smileyView)
        self.delegate?.howToPlayShown(index: self.index)
      }
    }
  }

  private func addPlayToScrollInfoView(_ smileyView: UIView) {
    contentView.addSubview(playToScrollInfoView)
    NSLayoutConstraint.activate([
      playToScrollInfoView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.onbaordingInfoViewHorizontalSpacing
      ),
      playToScrollInfoView.trailingAnchor.constraint(
        equalTo: contentView.trailingAnchor,
        constant: -Constants.onbaordingInfoViewHorizontalSpacing
      ),
      playToScrollInfoView.bottomAnchor.constraint(
        equalTo: smileyView.topAnchor,
        constant: Constants.onbaordingInfoViewBottom
      ),
      playToScrollInfoView.heightAnchor.constraint(equalToConstant: Constants.howToPlayInfoViewHeight)
    ])
    if let overlayView = signupOverlayHost.view {
      contentView.bringSubviewToFront(overlayView)
    }
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissPlayToScrollOnTap))
    playToScrollInfoView.addGestureRecognizer(tapGesture)

    let translateAnimation = CABasicAnimation(keyPath: "transform.translation.y")
    translateAnimation.fromValue = -Constants.onboardingInfoAnimationTranslation
    translateAnimation.toValue =  Constants.onboardingInfoAnimationTranslation
    translateAnimation.duration = CGFloat.half
    translateAnimation.autoreverses = true
    translateAnimation.repeatCount = .greatestFiniteMagnitude
    translateAnimation.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

    playToScrollInfoView.layer.add(translateAnimation, forKey: "onboardingBob")
  }

  @objc func dismissPlayToScrollOnTap() {
    guard let smileyView = smileyGameHostController?.view else { return }
    self.cleanupOnOnboardingCompletion(smileyView: smileyView)
    self.delegate?.howToPlayShown(index: index)
  }
}
