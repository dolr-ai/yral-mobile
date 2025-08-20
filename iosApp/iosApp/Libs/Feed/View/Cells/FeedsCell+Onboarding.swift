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
    guard !isOnboardingVisible, let smileyView = smileyGameHostController?.view else { return }
    isOnboardingVisible = true
    addGameInfoOverlayView()
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissOnboardingOnTap))
    gameInfoOverlayView.addGestureRecognizer(tapGesture)
    animateGameInfoOverlayView(smileyView: smileyView) { [weak self] in
      guard let self = self else { return }
      self.addOnboardingInfoView(smileyView)

      self.onboardingDismissWorkItem?.cancel()
      let work = DispatchWorkItem { [weak self] in
        guard let self = self, let view = self.smileyGameHostController?.view else { return }
        self.cleanupOnOnboardingCompletion(smileyView: view)
      }
      self.onboardingDismissWorkItem = work
      DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(.five), execute: work)
    }
  }

  func highlightSmileyView(_ smileyView: UIView) {
    smileyView.layer.masksToBounds = false
    smileyView.layer.cornerRadius = Constants.smileyGameHeight / CGFloat.two
    smileyView.layer.shadowColor = Constants.smileyGameShadowColor
    smileyView.layer.shadowOffset = .zero
    smileyView.layer.shadowRadius = Constants.smileyShadowMaxRadius
    smileyView.layer.shadowOpacity = 0.0
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
  }

  func animateGameInfoOverlayView(smileyView: UIView, completion: @escaping () -> Void) {
    UIView.animate(withDuration: CGFloat.one) { [weak self] in
      guard let self = self else { return }
      self.gameInfoOverlayView.alpha = .one
      highlightSmileyView(smileyView)
      self.contentView.layoutIfNeeded()
    } completion: { _ in
      self.animatePublisher.send(SmileyGameConfig.shared.config)
      completion()
    }
  }

  func addGameInfoOverlayView() {
    contentView.addSubview(gameInfoOverlayView)
    gameInfoOverlayView.backgroundColor = Constants.onboardingBGColor
    guard let smileyView = smileyGameHostController?.view else { return }
    contentView.bringSubviewToFront(smileyView)
    NSLayoutConstraint.activate([
      gameInfoOverlayView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
      gameInfoOverlayView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
      gameInfoOverlayView.topAnchor.constraint(equalTo: contentView.topAnchor),
      gameInfoOverlayView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor)
    ])
    UIView.performWithoutAnimation {
      contentView.layoutIfNeeded()
    }
    gameInfoOverlayView.alpha = .zero
  }

  private func addOnboardingInfoView(_ smileyView: UIView) {
    contentView.addSubview(onboardingInfoView)
    NSLayoutConstraint.activate([
      onboardingInfoView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.onbaordingInfoViewHorizontalSpacing
      ),
      onboardingInfoView.trailingAnchor.constraint(
        equalTo: contentView.trailingAnchor,
        constant: -Constants.onbaordingInfoViewHorizontalSpacing
      ),
      onboardingInfoView.bottomAnchor.constraint(
        equalTo: smileyView.topAnchor,
        constant: Constants.onbaordingInfoViewBottom
      ),
      onboardingInfoView.heightAnchor.constraint(equalToConstant: Constants.onbaordingInfoViewHeight)
    ])
    addStarts()
    contentView.bringSubviewToFront(onboardingInfoView)
    if let overlayView = signupOverlayHost.view {
      contentView.bringSubviewToFront(overlayView)
    }
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissOnboardingOnTap))
    onboardingInfoView.addGestureRecognizer(tapGesture)

    let translateAnimation = CABasicAnimation(keyPath: "transform.translation.y")
    translateAnimation.fromValue = -Constants.onboardingInfoAnimationTranslation
    translateAnimation.toValue =  Constants.onboardingInfoAnimationTranslation
    translateAnimation.duration = CGFloat.half
    translateAnimation.autoreverses = true
    translateAnimation.repeatCount = .greatestFiniteMagnitude
    translateAnimation.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)

    onboardingInfoView.layer.add(translateAnimation, forKey: "onboardingBob")

    let opacityAnimation = CABasicAnimation(keyPath: "opacity")
    opacityAnimation.fromValue = CGFloat.one
    opacityAnimation.toValue = CGFloat.zero
    opacityAnimation.duration = translateAnimation.duration
    opacityAnimation.autoreverses = true
    opacityAnimation.repeatCount = translateAnimation.repeatCount
    opacityAnimation.timingFunction = translateAnimation.timingFunction
    starsView.layer.add(opacityAnimation, forKey: "starsAppearDisappear")
  }

  private func addStarts() {
    contentView.addSubview(starsView)
    NSLayoutConstraint.activate([
      starsView.topAnchor.constraint(equalTo: onboardingInfoView.topAnchor, constant: -Constants.starsImageTop),
      starsView.heightAnchor.constraint(equalToConstant: Constants.starsImageViewHeight),
      starsView.leadingAnchor.constraint(
        equalTo: onboardingInfoView.leadingAnchor,
        constant: OnboardingInfoView.Constants.horizontalSpacing
      ),
      starsView.trailingAnchor.constraint(
        equalTo: onboardingInfoView.trailingAnchor,
        constant: -OnboardingInfoView.Constants.horizontalSpacing
      )
    ])
  }

  func addHowToPlay() {
    if howToPlayButton.superview == nil {
      contentView.addSubview(howToPlayButton)
      howToPlayWidthAnchor = howToPlayButton.widthAnchor.constraint(equalToConstant: Constants.howToPlayCollapsedWidth)
      NSLayoutConstraint.activate([
        howToPlayButton.leadingAnchor.constraint(
          equalTo: contentView.leadingAnchor,
          constant: Constants.smileyGameHorizontal
        ),
        howToPlayButton.heightAnchor.constraint(equalToConstant: Constants.howToPlayHeight),
        howToPlayWidthAnchor!,
        howToPlayButton.centerYAnchor.constraint(equalTo: actionsStackView.centerYAnchor)
      ])
      howToPlayButton.addTarget(self, action: #selector(howToPlayTapped), for: .touchUpInside)
    } else {
      howToPlayWidthAnchor?.constant = Constants.howToPlayCollapsedWidth
    }
    UIView.performWithoutAnimation { self.contentView.layoutIfNeeded() }
    if self.index < Int.three {
      DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.one) {
        UIView.animate(
          withDuration: CGFloat.pointSevenFive,
          delay: CGFloat.zero,
          usingSpringWithDamping: CGFloat.one,
          initialSpringVelocity: CGFloat.half,
          options: [.curveEaseInOut, .allowUserInteraction, .beginFromCurrentState]
        ) { [weak self] in
          guard let self = self else { return }
          self.howToPlayButton.setImage(UIImage(named: Constants.howToPlayImageExpanded), for: .normal)
          self.howToPlayWidthAnchor?.constant = Constants.howToPlayExpandedWidth
          self.contentView.layoutIfNeeded()
        } completion: { [weak self] _ in
          guard let self = self else { return }
          DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.two) {
            UIView.animate(
              withDuration: CGFloat.two,
              delay: CGFloat.zero,
              usingSpringWithDamping: CGFloat.one,
              initialSpringVelocity: CGFloat.pointOne,
              options: [.curveEaseInOut, .allowUserInteraction, .beginFromCurrentState]
            ) { [weak self] in
              guard let self = self else { return }
              self.howToPlayButton.setImage(UIImage(named: Constants.howToPlayImageCollapsed), for: .normal)
              self.howToPlayWidthAnchor?.constant = Constants.howToPlayCollapsedWidth
              self.contentView.layoutIfNeeded()
            }
          }
        }
      }
    }
  }

  @objc func dismissOnboardingOnTap() {
    onboardingDismissWorkItem?.cancel()
    onboardingDismissWorkItem = nil
    guard let smileyView = smileyGameHostController?.view else { return }
    self.cleanupOnOnboardingCompletion(smileyView: smileyView)
  }

  @objc func howToPlayTapped() {
    self.delegate?.howToPlayTapped()
  }

  func cleanupOnOnboardingCompletion(smileyView: UIView) {
    self.animationCompletionPublisher.send()
    smileyView.layer.shadowOpacity = .zero
    smileyView.backgroundColor = .clear
    smileyView.layer.removeAnimation(forKey: "smileyShadowPulse")
    self.onboardingInfoView.layer.removeAnimation(forKey: "onboardingBob")
    self.playToScrollInfoView.layer.removeAnimation(forKey: "onboardingBob")
    self.starsView.layer.removeAnimation(forKey: "starsAppearDisappear")
    self.gameInfoOverlayView.removeFromSuperview()
    self.starsView.removeFromSuperview()
    self.onboardingInfoView.removeFromSuperview()
    self.playToScrollInfoView.removeFromSuperview()
    howToPlayWidthAnchor?.isActive = false
    howToPlayWidthAnchor = nil
    howToPlayButton.removeFromSuperview()
    howToPlayButton.setImage(UIImage(named: Constants.howToPlayImageCollapsed), for: .normal)
    isOnboardingVisible = false
    contentView.layoutIfNeeded()
  }
}
