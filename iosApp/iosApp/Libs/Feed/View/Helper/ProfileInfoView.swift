//
//  ProfileInfoView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

// swiftlint: disable type_body_length
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

  var coinsView: UIView = {
    let view = getUIView()
    view.setGradientBackground(
      colors: [
        Constants.coinsViewGradientStartColor,
        Constants.coinsViewGradientEndColor
      ],
      frame: CGRect(x: .zero,
                    y: .zero,
                    width: Constants.coinsViewMinimumWidth,
                    height: Constants.coinsViewHeight),
      cornerRadius: Constants.coinsViewHeight / 2,
      opacity: Constants.coinsViewOpacity)
    view.clipsToBounds = false
    return view
  }()

  var coinsLabel: UILabel = {
    let label = getUILabel()
    label.backgroundColor = .clear
    label.font = Constants.coinsLabelFont
    label.textColor = Constants.coinsLabelTextColor
    return label
  }()

  var coinsBagImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.coinsBagFilledImage
    return imageView
  }()

  var coinsImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.coinsImage
    return imageView
  }()

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
    addCoinView()
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

  func addCoinView() {
    addSubview(coinsImageView)
    addSubview(coinsView)
    coinsView.addSubview(coinsLabel)
    coinsView.addSubview(coinsBagImageView)

    coinsImageView.alpha = 0

    NSLayoutConstraint.activate([
      coinsView.trailingAnchor.constraint(equalTo: trailingAnchor),
      coinsView.centerYAnchor.constraint(equalTo: imageView.centerYAnchor),
      coinsView.heightAnchor.constraint(equalToConstant: Constants.coinsViewHeight),
      coinsView.widthAnchor.constraint(greaterThanOrEqualToConstant: Constants.coinsViewMinimumWidth),

      coinsLabel.trailingAnchor.constraint(equalTo: coinsView.trailingAnchor,
                                           constant: -Constants.coinsLabelTrailing),
      coinsLabel.leadingAnchor.constraint(equalTo: coinsView.leadingAnchor,
                                          constant: Constants.coinsLabelLeading),
      coinsLabel.centerYAnchor.constraint(equalTo: coinsView.centerYAnchor),

      coinsBagImageView.widthAnchor.constraint(equalToConstant: Constants.coinsBagImageSize),
      coinsBagImageView.heightAnchor.constraint(equalToConstant: Constants.coinsBagImageSize),
      coinsBagImageView.centerYAnchor.constraint(equalTo: coinsView.centerYAnchor),
      coinsBagImageView.trailingAnchor.constraint(equalTo: coinsLabel.leadingAnchor,
                                                  constant: -Constants.coinsBagTrailing),

      coinsImageView.trailingAnchor.constraint(equalTo: coinsView.leadingAnchor,
                                               constant: -Constants.coinsImageLeading),
      coinsImageView.widthAnchor.constraint(equalToConstant: Constants.coinsImageSize),
      coinsImageView.heightAnchor.constraint(equalToConstant: Constants.coinsImageSize),
      coinsImageView.topAnchor.constraint(equalTo: coinsView.topAnchor,
                                          constant: -Constants.coinsImageTop)
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
    coinsLabel.text = "\(data.coins)"
  }

  func update(coins: Int) {
    guard var profileInfo else { return }

    if coins > profileInfo.coins {
      profileInfo.coins = coins
      incrementCoins()
    } else if coins < profileInfo.coins {
      profileInfo.coins = coins
      decrementCoins()
    }
  }

  func incrementCoins() {
    UIView.animate(withDuration: 0.5) {
      self.coinsImageView.alpha = 1
      self.layoutIfNeeded()
    } completion: { complete in
      if complete {
        UIView.animate(withDuration: 0.5, delay: 0.25) {
          self.coinsBagImageView.transform = CGAffineTransform
            .identity
            .scaledBy(x: 1.4, y: 1.4)
            .rotated(by: -15.53 * .pi / 180)
          self.coinsBagImageView.layer.opacity = 0.82
          self.coinsImageView.transform = CGAffineTransform
            .identity
            .scaledBy(x: 0.5, y: 0.5)
            .translatedBy(x: 80, y: -60)
          self.coinsImageView.alpha = 0
          self.coinsLabel.transform = CGAffineTransform
            .identity
            .translatedBy(x: 0, y: -10)
          self.coinsLabel.alpha = 0
          self.layoutIfNeeded()
        } completion: { complete in
          if complete {
            self.coinsImageView.transform = .identity
            self.coinsLabel.transform = CGAffineTransform
              .identity
              .translatedBy(x: 0, y: 10)
            self.coinsLabel.textColor = YralColor.green400.uiColor
            UIView.animate(withDuration: 0.5) {
              self.coinsBagImageView.transform = .identity
              self.coinsBagImageView.layer.opacity = 1
              self.coinsLabel.transform = .identity
              self.coinsLabel.alpha = 1
              self.layoutIfNeeded()
            } completion: { complete in
              if complete {
                UIView.animate(withDuration: 0.5) {
                  self.coinsLabel.textColor = Constants.coinsLabelTextColor
                } completion: { complete in
                  if complete {

                  }
                }

              }
            }

          }
        }

      }
    }

  }

  func decrementCoins() {
    self.coinsImageView.transform = CGAffineTransform
      .identity
      .scaledBy(x: 0.5, y: 0.5)
      .translatedBy(x: 80, y: -60)
    UIView.animate(withDuration: 0.5) {
      self.coinsImageView.alpha = 1
      self.layoutIfNeeded()
    } completion: { complete in
      if complete {
        UIView.animate(withDuration: 0.5, delay: 0.25) {
          self.coinsBagImageView.transform = CGAffineTransform
            .identity
            .scaledBy(x: 1.4, y: 1.4)
            .rotated(by: -15.53 * .pi / 180)
          self.coinsBagImageView.layer.opacity = 0.82
          self.coinsImageView.transform = .identity
          self.coinsImageView.alpha = 0
          self.coinsLabel.transform = CGAffineTransform
            .identity
            .translatedBy(x: 0, y: -10)
          self.coinsLabel.alpha = 0
          self.layoutIfNeeded()
        } completion: { complete in
          if complete {
            self.coinsImageView.transform = .identity
            self.coinsLabel.transform = CGAffineTransform
              .identity
              .translatedBy(x: 0, y: 10)
            self.coinsLabel.textColor = YralColor.red300.uiColor
            UIView.animate(withDuration: 0.5) {
              self.coinsBagImageView.transform = .identity
              self.coinsBagImageView.layer.opacity = 1
              self.coinsLabel.transform = .identity
              self.coinsLabel.alpha = 1
              self.layoutIfNeeded()
            } completion: { complete in
              if complete {
                UIView.animate(withDuration: 0.5) {
                  self.coinsLabel.textColor = Constants.coinsLabelTextColor
                } completion: { complete in
                  if complete {

                  }
                }

              }
            }

          }
        }

      }
    }
  }

  struct ProfileInfo {
    let imageURL: URL?
    let title: String
    let subtitle: String
    var coins: Int
  }
}
// swiftlint: enable type_body_length

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
    static let labelTrailing = 115.0
    static let defaultProfileImage = UIImage(named: "default_profile")
    static let coinsViewHeight = 32.0
    static let coinsViewMinimumWidth = 75.0
    static let coinsViewGradientStartColor = UIColor(red: 255/255,
                                                     green: 205/255,
                                                     blue: 0,
                                                     alpha: 1)
    static let coinsViewGradientEndColor = UIColor(red: 218/255,
                                                   green: 129/255,
                                                   blue: 0,
                                                   alpha: 1)
    static let coinsViewOpacity: Float = 0.8
    static let coinsLabelFont = YralFont.pt16.semiBold.uiFont
    static let coinsLabelTextColor = YralColor.grey950.uiColor
    static let coinsLabelTrailing = 10.0
    static let coinsLabelLeading = 22.0
    static let coinsBagImageSize = 36.0
    static let coinsBagTrailing = 2.0
    static let coinsBagFilledImage = UIImage(named: "coin_bag_filled")
    static let coinsImageSize = 67.0
    static let coinsImageLeading = 18.0
    static let coinsImageTop = 8.0
    static let coinsImage = UIImage(named: "coins")
  }
}
