//
//  PlayToScrollInfoView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

class PlayToScrollInfoView: UIView {
  var infoLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.numberOfLines = .two
    label.text = Constants.infoLabelText
    label.font = Constants.font
    label.textColor = Constants.infoTextColor
    label.textAlignment = .center
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
    addInfoLabel()
    addArrow()
  }

  func addInfoLabel() {
    addSubview(infoLabel)
    NSLayoutConstraint.activate([
      infoLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: Constants.horizontalSpacing),
      infoLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -Constants.horizontalSpacing),
      infoLabel.topAnchor.constraint(equalTo: self.topAnchor)
    ])
  }

  func addArrow() {
    addSubview(arrowImageView)
    NSLayoutConstraint.activate([
      arrowImageView.centerXAnchor.constraint(equalTo: self.centerXAnchor),
      arrowImageView.widthAnchor.constraint(equalToConstant: Constants.imageViewWidth),
      arrowImageView.heightAnchor.constraint(equalToConstant: Constants.imageViewHeight),
      arrowImageView.topAnchor.constraint(equalTo: infoLabel.bottomAnchor, constant: Constants.verticalSpacing)
    ])
  }
}

extension PlayToScrollInfoView {
  enum Constants {
    static let infoLabelText = "React to this video\n to unlock the next one!"

    static let infoTextColor = YralColor.grey50.uiColor
    static let font = YralFont.pt20.bold.uiFont
    static let imageName = "onboarding_arrow"

    static let horizontalSpacing = 12.0
    static let verticalSpacing = 8.0
    static let imageViewHeight = 152.0
    static let imageViewWidth = 42.0
  }
}
