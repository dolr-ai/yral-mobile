//
//  ToastView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

final class ToastView: UIView {

  private let type: ToastType
  private let buttonAction: (() -> Void)?

  private let iconImageView = getUIImageView()
  private let titleLabel = getUILabel()
  private let subtitleLabel = getUILabel()
  private let actionButton = getUIButton()
  private let closeButton = UIButton(type: .system)

  private var initialOriginY: CGFloat = 0

  init(type: ToastType, buttonAction: (() -> Void)? = nil) {
    self.type = type
    self.buttonAction = buttonAction
    super.init(frame: .zero)

    backgroundColor = type.backgroundColor
    layer.masksToBounds = true
    layer.cornerRadius = Constants.cornerRadius
    setupUI()
    setupPanGesture()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    setupCloseButton()
    setupIconImageView()
    setupTitleLabel()
    setupSubtitleLabel()
    setupActionButtonIfNeeded()
  }

  private func setupCloseButton() {
    closeButton.setImage(Constants.closeButtonImage, for: .normal)
    closeButton.tintColor = .white
    closeButton.addTarget(self, action: #selector(handleCloseButtonTapped), for: .touchUpInside)
    addSubview(closeButton)

    closeButton.translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activate([
      closeButton.topAnchor.constraint(equalTo: topAnchor, constant: Constants.topPadding),
      closeButton.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Constants.horizontalPadding)
    ])
  }

  private func setupIconImageView() {
    iconImageView.contentMode = .scaleAspectFit
    iconImageView.image = type.icon
    addSubview(iconImageView)

    NSLayoutConstraint.activate([
      iconImageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Constants.horizontalPadding),
      iconImageView.topAnchor.constraint(equalTo: topAnchor, constant: Constants.topPadding),
      iconImageView.widthAnchor.constraint(equalToConstant: Constants.iconSize),
      iconImageView.heightAnchor.constraint(equalToConstant: Constants.iconSize)
    ])
  }

  private func setupTitleLabel() {
    titleLabel.text = type.title
    titleLabel.font = Constants.titleLabelFont
    titleLabel.textColor = Constants.titleLabelTextColor
    addSubview(titleLabel)

    NSLayoutConstraint.activate([
      titleLabel.topAnchor.constraint(equalTo: topAnchor, constant: Constants.topPadding),
      titleLabel.leadingAnchor.constraint(equalTo: iconImageView.trailingAnchor, constant: Constants.spacing),
      titleLabel.trailingAnchor.constraint(
        lessThanOrEqualTo: closeButton.leadingAnchor,
        constant: -Constants.spacing
      )
    ])
  }

  private func setupSubtitleLabel() {
    subtitleLabel.text = type.subtitle
    subtitleLabel.font = Constants.subtitleLabelFont
    subtitleLabel.textColor = type.subtitleColor
    subtitleLabel.numberOfLines = .zero
    addSubview(subtitleLabel)

    NSLayoutConstraint.activate([
      subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: .two),
      subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
      subtitleLabel.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Constants.horizontalPadding)
    ])
  }

  private func setupActionButtonIfNeeded() {
    guard let btnTitle = type.buttonTitle else {
      NSLayoutConstraint.activate([
        subtitleLabel.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Constants.topPadding)
      ])
      return
    }

    actionButton.setTitle(btnTitle, for: .normal)
    actionButton.setTitleColor(Constants.retryButtonTextColor, for: .normal)
    actionButton.titleLabel?.font = Constants.retryButtonFont
    actionButton.addTarget(self, action: #selector(handleActionButtonTapped), for: .touchUpInside)
    addSubview(actionButton)

    NSLayoutConstraint.activate([
      actionButton.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: Constants.spacing),
      actionButton.leadingAnchor.constraint(equalTo: subtitleLabel.leadingAnchor),
      actionButton.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Constants.topPadding)
    ])
  }

  private func setupPanGesture() {
    let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handlePanGesture(_:)))
    addGestureRecognizer(panGesture)
  }

  @objc private func handlePanGesture(_ gesture: UIPanGestureRecognizer) {
    let translation = gesture.translation(in: superview)

    switch gesture.state {
    case .began:
      initialOriginY = frame.origin.y

    case .changed:
      if translation.y < .zero {
        frame.origin.y = initialOriginY + translation.y
      }
    case .ended, .cancelled:
      let dragAmount = initialOriginY - frame.origin.y
      let velocity = gesture.velocity(in: superview).y
      let dismissThreshold = CGFloat.animationPeriod * bounds.height
      if dragAmount > dismissThreshold || velocity < Constants.panGestureThreshold {
        dismiss()
      } else {
        UIView.animate(withDuration: CGFloat.apiDelay) {
          self.frame.origin.y = self.initialOriginY
        }
      }
    default:
      break
    }
  }

  @objc private func handleActionButtonTapped() {
    buttonAction?()
    dismiss()
  }

  @objc private func handleCloseButtonTapped() {
    dismiss()
  }

  private func dismiss() {
    UIView.animate(withDuration: 0.3, animations: {
      self.frame.origin.y = -self.frame.height
    }, completion: { _ in
      self.removeFromSuperview()
    })
  }
}

extension ToastView {
  enum Constants {
    static let closeButtonImage = UIImage(named: "toast_close")
    static let titleLabelTextColor = YralColor.grey50.uiColor
    static let titleLabelFont = YralFont.pt14.medium.uiFont
    static let subtitleLabelTextColor = YralColor.grey300.uiColor
    static let subtitleLabelFont = YralFont.pt14.uiFont
    static let retryButtonTextColor = YralColor.red400.uiColor
    static let retryButtonFont = YralFont.pt14.medium.uiFont
    static let cornerRadius = 6.0
    static let topPadding: CGFloat = 12
    static let horizontalPadding: CGFloat = 16
    static let spacing: CGFloat = 8
    static let iconSize: CGFloat = 24
    static let panGestureThreshold = -500.0
  }
}
