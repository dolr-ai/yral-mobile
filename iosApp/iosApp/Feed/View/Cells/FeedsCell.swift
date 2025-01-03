//
//  FeedsCell.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  private let playerContainerView = getUIImageView()

  private var actionsStackView: UIStackView = {
    let stackView = getUIStackView()
    stackView.axis = .vertical
    stackView.distribution = .fillEqually
    stackView.spacing = Constants.stackViewSpacing
    stackView.backgroundColor = Constants.stackViewBGColor
    stackView.alignment = .trailing
    return stackView
  }()

  private var likeButton: UIButton = {
    getActionButton(withTitle: "100", image: Constants.likeButtonImage)
  }()

  private var shareButton: UIButton = {
    return getActionButton(withTitle: "100k", image: Constants.shareButtonImage)
  }()

  private static func getActionButton(withTitle title: String, image: UIImage?) -> UIButton {
    var configuration = UIButton.Configuration.plain()
    configuration.image = image ?? UIImage()
    configuration.imagePlacement = .top
    configuration.titleAlignment = .center
    configuration.imagePadding = Constants.actionButtonImagePadding
    configuration.baseForegroundColor = Constants.actionButtonTitleColor
    configuration.contentInsets = .zero
    configuration.attributedTitle = AttributedString(title, attributes: AttributeContainer([
      .font: Constants.actionButtonFont
    ]))
    configuration.titleLineBreakMode = .byTruncatingTail
    let button = UIButton(type: .custom)
    button.translatesAutoresizingMaskIntoConstraints = false
    button.configuration = configuration
    button.titleLabel?.numberOfLines = .one
    button.backgroundColor = .clear
    button.widthAnchor.constraint(equalToConstant: Constants.actionButtonWidth).isActive = true
    button.heightAnchor.constraint(equalToConstant: Constants.actionButtonHeight).isActive = true
    return button
  }

  private var profileInfoView: ProfileInfoView = {
    let profileInfoView = ProfileInfoView()
    profileInfoView.heightAnchor.constraint(equalToConstant: Constants.profileInfoViewHeight).isActive = true
    profileInfoView.layer.masksToBounds = true
    profileInfoView.layer.cornerRadius = Constants.profileInfoViewHeight / 2
    return profileInfoView
  }()

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    addPlayerContainerView()
    setupProfileInfoView()
    setupStackView()
  }

  func addPlayerContainerView() {
    contentView.addSubview(playerContainerView)
    NSLayoutConstraint.activate([
      playerContainerView.topAnchor.constraint(equalTo: contentView.topAnchor),
      playerContainerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
      playerContainerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
      playerContainerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor)
    ])
    contentView.layoutIfNeeded()
  }

  func setupProfileInfoView() {
    contentView.addSubview(profileInfoView)
    NSLayoutConstraint.activate([
      profileInfoView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.profileInfoLeading
      ),
      profileInfoView.trailingAnchor.constraint(
        equalTo: contentView.trailingAnchor,
        constant: -Constants.profileInfoTrailing
      ),
      profileInfoView.topAnchor.constraint(
        equalTo: contentView.topAnchor,
        constant: Constants.profileInfoTop
      )
    ])
  }

  func setupStackView() {
    contentView.addSubview(actionsStackView)
    NSLayoutConstraint.activate([
      actionsStackView.trailingAnchor.constraint(
        equalTo: contentView.trailingAnchor,
        constant: -Constants.horizontalMargin
      ),
      actionsStackView.widthAnchor.constraint(equalToConstant: Constants.actionButtonWidth),
      actionsStackView.heightAnchor.constraint(equalToConstant: Constants.stackViewHeight),
      actionsStackView.bottomAnchor.constraint(
        equalTo: contentView.safeAreaLayoutGuide.bottomAnchor,
        constant: -Constants.stackViewBottom
      )
    ])
    actionsStackView.addArrangedSubview(likeButton)
    actionsStackView.addArrangedSubview(shareButton)
  }

  func configure(
    withPlayer player: AVPlayer,
    thumbnailURL: URL,
    lastFrameImage: UIImage?,
    profileInfo: ProfileInfoView.ProfileInfo
  ) {
    if let lastFrameImage {
      playerContainerView.image = lastFrameImage
    } else {
      loadImage(with: thumbnailURL, on: playerContainerView)
    }
    playerLayer?.removeFromSuperlayer()
    let layer = AVPlayerLayer(player: player)
    layer.videoGravity = .resize
    playerContainerView.layer.addSublayer(layer)
    playerLayer = layer
    playerLayer?.frame = contentView.bounds
    profileInfoView.set(data: profileInfo)
  }

  override func layoutSubviews() {
    playerLayer?.frame = playerContainerView.bounds
  }

  override func prepareForReuse() {
      super.prepareForReuse()
      playerLayer?.player = nil
  }
}

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 24.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 126.0
    static let stackViewBottom = 74.0
    static let stackViewBGColor = UIColor.clear
    static let actionButtonFont = UIFont(name: "KumbhSans-SemiBold", size: 15) ?? UIFont.systemFont(ofSize: 15)
    static let likeButtonImage = UIImage(named: "like_feed")
    static let shareButtonImage = UIImage(named: "share_feed")
    static let actionButtonHeight: CGFloat = 51.0
    static let actionButtonWidth: CGFloat = 34.0
    static let actionButtonImagePadding = 4.0
    static let actionButtonTitleColor = UIColor.white
    static let profileInfoLeading = 16.0
    static let profileInfoTop = 8.0
    static let profileInfoTrailing = 60.0
    static let profileInfoViewHeight = 56.0
  }
}
