//
//  ProfileInfoView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

class ProfileInfoView: UIView, ImageLoaderProtocol {
  var positionBackgroundView: UIView = {
    let view = getUIView()
    view.setGradientBackground(
      colors: [
        YralColor.red300.uiColor,
        YralColor.red500.uiColor
      ],
      frame: CGRect(x: .zero,
                    y: .zero,
                    width: 75,
                    height: 32),
      cornerRadius: 16,
      opacity: 1
    )
    view.clipsToBounds = false
    return view
  }()

  var imageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.backgroundColor = .clear
    imageView.layer.masksToBounds = true
    return imageView
  }()

  var positionLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.font = YralFont.pt16.semiBold.uiFont
    label.textColor = YralColor.grey50.uiColor
    label.adjustsFontSizeToFitWidth = true
    label.minimumScaleFactor = 0.5
    label.numberOfLines = 1
    return label
  }()

  let coinsView = CoinsView()

  var profileInfo: ProfileInfo?
  weak var delegate: ProfileInfoViewDelegate?

  override init(frame: CGRect) {
    super.init(frame: .zero)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  func setupUI() {
    self.translatesAutoresizingMaskIntoConstraints = false
    addRankView()
    addCoinsView()
  }

  func addRankView() {
    addSubview(positionBackgroundView)
    positionBackgroundView.addSubview(imageView)
    positionBackgroundView.addSubview(positionLabel)
    imageView.image = UIImage(named: "feed_trophy")

    NSLayoutConstraint.activate([
      positionBackgroundView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
      positionBackgroundView.centerYAnchor.constraint(equalTo: centerYAnchor),
      positionBackgroundView.heightAnchor.constraint(equalToConstant: 32),
      positionBackgroundView.widthAnchor.constraint(greaterThanOrEqualToConstant: 75),

      imageView.topAnchor.constraint(equalTo: topAnchor, constant: Constants.imageViewTop),
      imageView.leadingAnchor.constraint(equalTo: positionBackgroundView.leadingAnchor, constant: -14),
      imageView.heightAnchor.constraint(equalToConstant: 38),
      imageView.widthAnchor.constraint(equalToConstant: 32),

      positionLabel.trailingAnchor.constraint(equalTo: positionBackgroundView.trailingAnchor, constant: -16),
      positionLabel.leadingAnchor.constraint(equalTo: positionBackgroundView.leadingAnchor, constant: 20),
      positionLabel.centerYAnchor.constraint(equalTo: positionBackgroundView.centerYAnchor)
    ])
  }

  func addCoinsView() {
    addSubview(coinsView)
    NSLayoutConstraint.activate([
      coinsView.trailingAnchor.constraint(equalTo: trailingAnchor),
      coinsView.centerYAnchor.constraint(equalTo: positionBackgroundView.centerYAnchor),
      coinsView.heightAnchor.constraint(equalToConstant: 36.0)
    ])
    coinsView.isUserInteractionEnabled = true
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(coinsTapped))
    tapGesture.numberOfTapsRequired = .one
    coinsView.addGestureRecognizer(tapGesture)
  }

  func set(data: ProfileInfo) {
    profileInfo = data
    coinsView.set(coins: data.coins)
    positionLabel.text = "#\(data.position.formattedWithSuffix)"
  }

  func updatePosition(newPosition: Int) {
    positionLabel.text = "#\(newPosition.formattedWithSuffix)"
  }

  func updateCoins(by newCoins: Int) {
    coinsView.updateCoins(by: newCoins)
  }

  @objc func coinsTapped() {
    delegate?.coinsTapped()
  }

  struct ProfileInfo {
    let imageURL: URL?
    let title: String
    let subtitle: String
    var coins: UInt64
    var position: Int
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

protocol ProfileInfoViewDelegate: AnyObject {
  func coinsTapped()
}
