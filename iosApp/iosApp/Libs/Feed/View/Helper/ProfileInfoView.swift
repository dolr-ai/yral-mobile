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

  let coinsView = CoinsView()

  var profileInfo: ProfileInfo?

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addImageView()
    addCoinsView()
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

  func addCoinsView() {
    addSubview(coinsView)
    NSLayoutConstraint.activate([
      coinsView.trailingAnchor.constraint(equalTo: trailingAnchor),
      coinsView.centerYAnchor.constraint(equalTo: imageView.centerYAnchor)
    ])
  }

  func addTitleLabel() {
    addSubview(titleLabel)
    NSLayoutConstraint.activate([
      titleLabel.leadingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: Constants.labelLeading),
      titleLabel.trailingAnchor.constraint(equalTo: coinsView.leadingAnchor, constant: -Constants.labelTrailing),
      titleLabel.heightAnchor.constraint(equalToConstant: Constants.titleLabelHeight),
      titleLabel.topAnchor.constraint(equalTo: imageView.topAnchor)
    ])
  }

  func addSubtitleLabel() {
    addSubview(subtitleLabel)
    NSLayoutConstraint.activate([
      subtitleLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor),
      subtitleLabel.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
      subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor),
      subtitleLabel.heightAnchor.constraint(equalToConstant: Constants.subtitleLabelHeight)
    ])
  }

  func set(data: ProfileInfo) {
    profileInfo = data

    if let imageURL = data.imageURL {
      loadImage(with: imageURL, placeholderImage: nil, on: imageView)
    } else {
      imageView.image = Constants.defaultProfileImage
    }
    titleLabel.text = data.title
    subtitleLabel.text = data.subtitle
    coinsView.set(coins: data.coins)
  }

  func updateCoins(by newCoins: Int) {
    coinsView.updateCoins(by: newCoins)
  }

  struct ProfileInfo {
    let imageURL: URL?
    let title: String
    let subtitle: String
    var coins: UInt64
  }
}

extension ProfileInfoView {
  enum Constants {
    static let imageViewSize = 40.0
    static let titleLabelHeight = 20.0
    static let subtitleLabelHeight = 16.0
    static let titleLabelFont = YralFont.pt16.semiBold.uiFont
    static let subtitleLabelFont = YralFont.pt12.medium.uiFont
    static let textColor = YralColor.grey0.uiColor
    static let imageBorderColor = YralColor.primary300.uiColor.cgColor
    static let imageBorderWidth = 2.0
    static let imageViewLeading = 8.0
    static let imageViewTop = 8.0
    static let labelLeading = 12.0
    static let labelTrailing = 12.0
    static let defaultProfileImage = UIImage(named: "default_profile")
  }
}
