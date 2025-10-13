//
//  CoinsView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 01/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

class CoinsView: UIView {

  var imageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.coinsImage
    return imageView
  }()

  var backgroundView: UIView = {
    let view = getUIView()
    view.setGradientBackground(
      colors: [
        Constants.backgroundGradientStartColor,
        Constants.backgroundGradientEndColor
      ],
      frame: CGRect(x: .zero,
                    y: .zero,
                    width: Constants.backgroundMinimumWidth,
                    height: Constants.backgroundHeight),
      cornerRadius: Constants.backgroundHeight / .two,
      opacity: Constants.backgroundOpacity
    )
    view.clipsToBounds = false
    return view
  }()

  var bagImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.bagFilledImage
    return imageView
  }()

  var label: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.font = Constants.labelFont
    label.textColor = Constants.labelTextColor
    label.adjustsFontSizeToFitWidth = true
    label.minimumScaleFactor = 0.5
    label.numberOfLines = 1
    return label
  }()

  var coins: UInt64 = .zero {
    didSet {
      label.text = "\(coins.formattedWithSuffix)"
    }
  }

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func resetUIState() {
    imageView.alpha = .zero
    imageView.transform = .identity
    bagImageView.alpha = .one
    bagImageView.transform = .identity
    label.alpha = .one
    label.transform = .identity
    label.textColor = Constants.labelTextColor

    layoutIfNeeded()

    imageView.layer.removeAllAnimations()
    bagImageView.layer.removeAllAnimations()
    label.layer.removeAllAnimations()
    layer.removeAllAnimations()
  }

  private func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addBackgroundView()
    addImageView()
  }

  private func addBackgroundView() {
    addSubview(backgroundView)
    backgroundView.addSubview(label)
    backgroundView.addSubview(bagImageView)

    NSLayoutConstraint.activate([
      backgroundView.trailingAnchor.constraint(equalTo: trailingAnchor),
      backgroundView.centerYAnchor.constraint(equalTo: centerYAnchor),
      backgroundView.heightAnchor.constraint(equalToConstant: Constants.backgroundHeight),
      backgroundView.widthAnchor.constraint(greaterThanOrEqualToConstant: Constants.backgroundMinimumWidth),

      label.trailingAnchor.constraint(equalTo: backgroundView.trailingAnchor, constant: -Constants.labelTrailing),
      label.leadingAnchor.constraint(equalTo: backgroundView.leadingAnchor, constant: Constants.labelLeading),
      label.centerYAnchor.constraint(equalTo: backgroundView.centerYAnchor),

      bagImageView.widthAnchor.constraint(equalToConstant: Constants.bagImageSize),
      bagImageView.heightAnchor.constraint(equalToConstant: Constants.bagImageSize),
      bagImageView.centerYAnchor.constraint(equalTo: backgroundView.centerYAnchor),
      bagImageView.trailingAnchor.constraint(equalTo: label.leadingAnchor, constant: -Constants.bagTrailing)
    ])
  }

  private func addImageView() {
    addSubview(imageView)
    imageView.alpha = .zero

    NSLayoutConstraint.activate([
      imageView.leadingAnchor.constraint(equalTo: leadingAnchor),
      imageView.trailingAnchor.constraint(equalTo: backgroundView.leadingAnchor, constant: -Constants.imageLeading),
      imageView.widthAnchor.constraint(equalToConstant: Constants.imageSize),
      imageView.heightAnchor.constraint(equalToConstant: Constants.imageSize),
      imageView.topAnchor.constraint(equalTo: backgroundView.topAnchor, constant: -Constants.imageTop)
    ])
  }

  private func incrementCoins(by newCoins: UInt64) {
    let imageViewTransform = CGAffineTransform
      .identity
      .scaledBy(x: Constants.imageScale, y: Constants.imageScale)
      .translatedBy(x: Constants.imageTranslationX, y: -Constants.imageTranslationY)

    animateIn {
      self.animateBagBounce(labelTintColor: Constants.labelWinTextColor, imageViewTransform: imageViewTransform) {
        self.coins += newCoins
        self.animationReset()
      }
    }
  }

  private func decrementCoins(by newCoins: UInt64) {
    imageView.transform = CGAffineTransform
      .identity
      .scaledBy(x: Constants.imageScale, y: Constants.imageScale)
      .translatedBy(x: Constants.imageTranslationX, y: -Constants.imageTranslationY)

    animateIn {
      self.animateBagBounce(labelTintColor: Constants.labelLoseTextColor, imageViewTransform: .identity) {
        self.coins -= newCoins
        self.animationReset()
      }
    }
  }

  func set(coins: UInt64) {
    self.coins = coins
  }

  func updateCoins(by newCoins: Int) {
    if newCoins > .zero {
      incrementCoins(by: UInt64(abs(newCoins)))
    } else if newCoins < .zero {
      decrementCoins(by: UInt64(abs(newCoins)))
    }
  }

  override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
    let hit = super.hitTest(point, with: event)
    if hit == self || subviews.contains(where: { $0 == hit }) {
      return self
    }
    return nil
  }
}

extension CoinsView {

  private func animate(duration: TimeInterval,
                       delay: TimeInterval = .zero,
                       options: UIView.AnimationOptions = [],
                       changes: @escaping () -> Void,
                       completion: ((Bool) -> Void)? = nil) {
    UIView.animate(
      withDuration: duration,
      delay: delay,
      options: options,
      animations: {
        changes()
        self.layoutIfNeeded()
      },
      completion: completion)
  }

  private func animateIn(completion: @escaping () -> Void) {
    animate(duration: Constants.animateInDuration) {
      self.imageView.alpha = .one
    } completion: { _ in
      completion()
    }
  }

  private func animateBagBounce(labelTintColor: UIColor,
                                imageViewTransform: CGAffineTransform,
                                completion: @escaping () -> Void) {
    animate(duration: Constants.animateBagDuration, delay: Constants.animateBagDelay) {
      self.bagImageView.transform = CGAffineTransform
        .identity
        .scaledBy(x: Constants.bagScale, y: Constants.bagScale)
        .rotated(by: Constants.bagRotationAngle)
      self.bagImageView.layer.opacity = Constants.bagOpacity
      self.imageView.transform = imageViewTransform
      self.imageView.alpha = .zero
      self.label.transform = CGAffineTransform
        .identity
        .translatedBy(x: Constants.labelTranslationX, y: -Constants.labelTranslationY)
      self.label.alpha = .zero
    } completion: { _ in
      self.imageView.transform = .identity
      self.label.transform = CGAffineTransform
        .identity
        .translatedBy(x: Constants.labelTranslationX, y: Constants.labelTranslationY)
      self.label.textColor = labelTintColor
      completion()
    }
  }

  private func animationReset() {
    animate(duration: Constants.animateResetDuration) {
      self.bagImageView.transform = .identity
      self.bagImageView.layer.opacity = 1
      self.label.transform = .identity
      self.label.alpha = .one
    } completion: { _ in
      self.animate(duration: Constants.animateResetDuration) {
        self.label.textColor = Constants.labelTextColor
      }
    }
  }
}

extension CoinsView {
  enum Constants {
    static let coinsImage = UIImage(named: "coins")
    static let imageSize = 67.0
    static let imageLeading = 18.0
    static let imageTop = 8.0

    static let backgroundGradientStartColor = UIColor(red: 255/255,
                                                      green: 205/255,
                                                      blue: 0,
                                                      alpha: 1)
    static let backgroundGradientEndColor = UIColor(red: 218/255,
                                                    green: 129/255,
                                                    blue: 0,
                                                    alpha: 1)
    static let backgroundMinimumWidth = 75.0
    static let backgroundHeight = 32.0
    static let backgroundOpacity: Float = 0.8

    static let bagFilledImage = UIImage(named: "coin_bag_filled")
    static let bagImageSize = 36.0
    static let bagTrailing = 2.0

    static let labelFont = YralFont.pt16.semiBold.uiFont
    static let labelTextColor = YralColor.grey950.uiColor
    static let labelWinTextColor = YralColor.green400.uiColor
    static let labelLoseTextColor = YralColor.red300.uiColor
    static let labelTrailing = 10.0
    static let labelLeading = 22.0

    static let animateInDuration = 0.5
    static let animateBagDuration = 0.5
    static let animateBagDelay = 0.25
    static let animateResetDuration = 0.5
    static let bagScale = 1.4
    static let bagRotationAngle = -15.53 * .pi / 180
    static let bagOpacity: Float = 0.82
    static let imageScale = 0.5
    static let imageTranslationX = 80.0
    static let imageTranslationY = 60.0
    static let labelTranslationX = 0.0
    static let labelTranslationY = 10.0
  }
}
