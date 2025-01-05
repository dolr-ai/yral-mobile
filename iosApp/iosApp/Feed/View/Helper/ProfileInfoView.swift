//
//  ProfileInfoView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

class ProfileInfoView: UIView, ImageLoaderProtocol {
  var imageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.backgroundColor = .clear
    imageView.layer.masksToBounds = true
    imageView.layer.borderWidth = Constants.imageBorderWidth
    imageView.layer.borderColor = Constants.imageBorderColor
    imageView.layer.cornerRadius = Constants.imageViewSize / 2
    return imageView
  }()

  var titleLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.font = Constants.titleLabelFont
    label.textColor = Constants.textColor
    return label
  }()

  var subtitleLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.font = Constants.subtitleLabelFont
    label.textColor = Constants.textColor
    return label
  }()

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func layoutSubviews() {
    addGradientLayer()
  }

  func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addImageView()
    addTitleLabel()
    addSubtitleLabel()
  }

  func addImageView() {
    addSubview(imageView)
    NSLayoutConstraint.activate([
      imageView.topAnchor.constraint(equalTo: topAnchor, constant: Constants.imageViewTop),
      imageView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Constants.imageViewLeading),
      imageView.heightAnchor.constraint(equalToConstant: Constants.imageViewSize),
      imageView.widthAnchor.constraint(equalToConstant: Constants.imageViewSize)
    ])
  }

  func addTitleLabel() {
    addSubview(titleLabel)
    NSLayoutConstraint.activate([
      titleLabel.leadingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: Constants.labelLeading),
      titleLabel.heightAnchor.constraint(equalToConstant: Constants.titleLabelHeight),
      titleLabel.topAnchor.constraint(equalTo: imageView.topAnchor)
    ])
  }

  func addSubtitleLabel() {
    addSubview(subtitleLabel)
    NSLayoutConstraint.activate([
      subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
      subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor),
      subtitleLabel.heightAnchor.constraint(equalToConstant: Constants.subtitleLabelHeight)
    ])
  }

  func addGradientLayer() {
    if let existingGradient = layer.sublayers?.first(where: { $0 is CAGradientLayer }) {
      existingGradient.frame = bounds
      return
    }
    let gradientLayer = CAGradientLayer()
    gradientLayer.frame = bounds
    gradientLayer.cornerRadius = layer.cornerRadius
    gradientLayer.colors = [Constants.gradientStartColor, Constants.gradientEndColor]
    gradientLayer.startPoint = CGPoint(x: .zero, y: .half)
    gradientLayer.endPoint = CGPoint(x: .one, y: .half)
    layer.insertSublayer(gradientLayer, at: .zero)
  }

  func set(data: ProfileInfo) {
    if let imageURL = data.imageURL {
      loadImage(with: imageURL, on: imageView)
    } else {
      imageView.image = Constants.defaultProfileImage
    }
    titleLabel.text = data.title
    subtitleLabel.text = data.subtitle
  }

  struct ProfileInfo {
    let imageURL: URL?
    let title: String
    let subtitle: String
  }
}

extension ProfileInfoView {
  enum Constants {
    static let imageViewSize = 40.0
    static let titleLabelHeight = 20.0
    static let subtitleLabelHeight = 16.0
    static let titleLabelFont = UIFont(name: "KumbhSans-SemiBold", size: 15.0)
    static let subtitleLabelFont = UIFont(name: "KumbhSans-Medium", size: 15.0)
    static let textColor = UIColor.white
    static let imageBorderColor = UIColor(red: 226/255, green: 1/255, blue: 123/255, alpha: 1.0).cgColor
    static let imageBorderWidth = 2.0
    static let imageViewLeading = 8.0
    static let imageViewTop = 8.0
    static let labelLeading = 12.0
    static let gradientStartColor = UIColor(red: 0, green: 0, blue: 0, alpha: 1).cgColor
    static let gradientEndColor = UIColor(red: 0.4, green: 0.4, blue: 0.4, alpha: 0).cgColor
    static let defaultProfileImage = UIImage(named: "default_profile")
  }
}
