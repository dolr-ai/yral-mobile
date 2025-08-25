//
//  OnboardingInfoView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

class OnboardingInfoView: UIView {
  var topLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.text = Constants.topLabelText
    label.font = Constants.font
    label.textColor = Constants.topLabelTextColor
    label.textAlignment = .center
    return label
  }()

  var bottomLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.text = Constants.bottomLabelText
    label.textColor = Constants.bottomLabelTextColor
    label.font = Constants.font
    label.textAlignment = .center
    label.numberOfLines = .two
    return label
  }()

  var arrowImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = UIImage(named: Constants.imageName)
    imageView.contentMode = .scaleToFill
    return imageView
  }()

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addTopLabel()
    addBottomLabel()
    addArrow()
  }

  func addTopLabel() {
    addSubview(topLabel)
    NSLayoutConstraint.activate([
      topLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: Constants.horizontalSpacing),
      topLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -Constants.horizontalSpacing),
      topLabel.topAnchor.constraint(equalTo: self.topAnchor)
    ])
  }

  func addBottomLabel() {
    addSubview(bottomLabel)
    NSLayoutConstraint.activate([
      bottomLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: Constants.horizontalSpacing),
      bottomLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -Constants.horizontalSpacing),
      bottomLabel.topAnchor.constraint(equalTo: topLabel.bottomAnchor, constant: Constants.verticalSpacing)
    ])
  }

  func addArrow() {
    addSubview(arrowImageView)
    NSLayoutConstraint.activate([
      arrowImageView.centerXAnchor.constraint(equalTo: self.centerXAnchor),
      arrowImageView.widthAnchor.constraint(equalToConstant: Constants.imageViewWidth),
      arrowImageView.heightAnchor.constraint(equalToConstant: Constants.imageViewHeight),
      arrowImageView.topAnchor.constraint(equalTo: bottomLabel.bottomAnchor, constant: Constants.verticalSpacing)
    ])
  }
}

extension OnboardingInfoView {
  enum Constants {
    static let topLabelText = "Match the vibe, win the coin"
    static let bottomLabelText = "Choose the top emojis & get YRAL (Bitcoin)"

    static let topLabelTextColor = YralColor.grey50.uiColor
    static let bottomLabelTextColor = YralColor.yellow200.uiColor
    static let font = YralFont.pt20.bold.uiFont
    static let imageName = "onboarding_arrow"

    static let horizontalSpacing = 12.0
    static let verticalSpacing = 8.0
    static let imageViewHeight = 152.0
    static let imageViewWidth = 42.0
  }
}
