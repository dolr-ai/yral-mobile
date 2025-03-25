//
//  FeedsCell.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

protocol FeedsCellProtocol: AnyObject {
  func shareButtonTapped(index: Int)
  func likeButtonTapped(index: Int)
}

class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  var index: Int = .zero
  weak var delegate: FeedsCellProtocol?
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

  var likeButton: UIButton = {
    getActionButton(withTitle: "100", image: Constants.likeUnSelectedImage)
  }()

  private var shareButton: UIButton = {
    return getActionButton(withTitle: "", image: Constants.shareButtonImage)
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
//    actionsStackView.addArrangedSubview(shareButton)
    likeButton.addTarget(self, action: #selector(likeButtonTapped), for: .touchUpInside)
    shareButton.addTarget(self, action: #selector(shareButtonTapped), for: .touchUpInside)
  }

  @objc func likeButtonTapped() {
    delegate?.likeButtonTapped(index: index)
  }

  @objc func shareButtonTapped() {
    delegate?.shareButtonTapped(index: index)
  }

  func configure(
    withPlayer player: AVPlayer,
    feedInfo: FeedCellInfo,
    profileInfo: ProfileInfoView.ProfileInfo,
    index: Int
  ) {
    if let lastThumbnailImage = feedInfo.lastThumbnailImage {
      playerContainerView.image = lastThumbnailImage
    } else if let thumbnailURL = feedInfo.thumbnailURL {
      loadImage(with: thumbnailURL, placeholderImage: Constants.playerPlaceHolderImage, on: playerContainerView)
    } else {
      playerContainerView.image = Constants.defaultProfileImage
    }
    playerLayer?.removeFromSuperlayer()
    let layer = AVPlayerLayer(player: player)
    layer.videoGravity = .resize
    playerContainerView.layer.addSublayer(layer)
    playerLayer = layer
    playerLayer?.frame = contentView.bounds
    profileInfoView.set(data: profileInfo)
    likeButton.configuration?.attributedTitle = AttributedString(
      String(feedInfo.likeCount),
      attributes: AttributeContainer([
        .font: Constants.actionButtonFont
      ])
    )
    likeButton.configuration?.image = feedInfo.isLiked ? Constants.likeSelectedImage : Constants.likeUnSelectedImage
    self.index = index
  }

  func setLikeStatus(isLiked: Bool) {
    likeButton.configuration?.image = isLiked ? Constants.likeSelectedImage : Constants.likeUnSelectedImage
    var likeButtonString = String((Int(likeButton.titleLabel?.text ?? "") ?? .zero) - .one)
    if isLiked {
      likeButtonString = String((Int(likeButton.titleLabel?.text ?? "") ?? .zero) + .one)
    }
    likeButton.configuration?.attributedTitle = AttributedString(
      likeButtonString,
      attributes: AttributeContainer([
        .font: Constants.actionButtonFont
      ])
    )
  }

  override func layoutSubviews() {
    playerLayer?.frame = playerContainerView.bounds
  }

  override func prepareForReuse() {
      super.prepareForReuse()
      playerLayer?.player = nil
  }

  struct FeedCellInfo {
    let thumbnailURL: URL?
    let likeCount: Int
    let isLiked: Bool
    let lastThumbnailImage: UIImage?
  }
}

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 14.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 106.0
    static let stackViewBottom = 74.0
    static let stackViewBGColor = UIColor.clear
    static let actionButtonFont = YralFont.pt16.semiBold.uiFont
    static let likeSelectedImage = UIImage(named: "like_selected_feed")
    static let likeUnSelectedImage = UIImage(named: "like_unselected_feed")
    static let shareButtonImage = UIImage(named: "share_feed")
    static let actionButtonHeight: CGFloat = 51.0
    static let actionButtonWidth: CGFloat = 34.0
    static let actionButtonImagePadding = 4.0
    static let actionButtonTitleColor = YralColor.grey50.uiColor
    static let profileInfoLeading = 16.0
    static let profileInfoTop = 8.0
    static let profileInfoTrailing = 60.0
    static let profileInfoViewHeight = 56.0
    static let defaultProfileImage = UIImage(named: "default_profile")
    static let playerPlaceHolderImage = UIImage(named: "player_placeholder")
  }
}
