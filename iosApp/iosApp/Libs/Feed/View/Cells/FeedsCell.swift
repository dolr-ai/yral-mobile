//
//  FeedsCell.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import SwiftUI
import AVFoundation

// swiftlint: disable type_body_length
protocol FeedsCellProtocol: AnyObject {
  func shareButtonTapped(index: Int)
  func likeButtonTapped(index: Int)
  func deleteButtonTapped(index: Int)
  func reportButtonTapped(index: Int)
  func loginTapped(provider: SocialProvider)
}
class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  var index: Int = .zero
  var feedType: FeedType = .otherUsers
  weak var delegate: FeedsCellProtocol?

  private let playerContainerView = getUIImageView()
  private lazy var signupOverlayHost = UIHostingController(
    rootView: SignupOverlay { provider in
      self.delegate?.loginTapped(provider: provider)
    }
  )

  var actionsStackView: UIStackView = {
    let stackView = getUIStackView()
    stackView.axis = .vertical
    stackView.distribution = .fill
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

  private var deleteButton: UIButton = {
    return getActionButton(withTitle: "", image: Constants.deleteButtonImage)
  }()

  private var reportButton: UIButton = {
    return getActionButton(withTitle: "", image: Constants.reportButtonImage)
  }()

  let captionScrollView: UIScrollView = {
    let scrollView = UIScrollView()
    scrollView.translatesAutoresizingMaskIntoConstraints = false
    scrollView.showsVerticalScrollIndicator = true
    scrollView.isScrollEnabled = false
    scrollView.showsVerticalScrollIndicator = false
    scrollView.showsHorizontalScrollIndicator = false
    return scrollView
  }()

  let captionLabel: UILabel = {
    let label = UILabel()
    label.translatesAutoresizingMaskIntoConstraints = false
    label.numberOfLines = 1
    label.lineBreakMode = .byTruncatingTail
    label.font = Constants.captionTitleFont
    label.textColor = Constants.captionTextColor
    label.textAlignment = .left
    return label
  }()

  var isCaptionExpanded = false
  var collapsedCaptionHeight: CGFloat = 0
  var expandedCaptionHeight: CGFloat = 0
  var isCaptionCollapsible = false
  var captionScrollViewHeightConstraint: NSLayoutConstraint!

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
    button.titleLabel?.numberOfLines = 1
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
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleFirstFrameReady(_:)),
      name: .feedItemReady,
      object: nil
    )
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    addPlayerContainerView()
    setupProfileInfoView()
    setupStackView()
    setupCaptionLabel()
    addSignupOverlay()

    let cellTapGesture = UITapGestureRecognizer(target: self, action: #selector(handleCellTap))
    cellTapGesture.cancelsTouchesInView = false
    contentView.addGestureRecognizer(cellTapGesture)
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
    // actionsStackView.addArrangedSubview(shareButton)
    likeButton.addTarget(self, action: #selector(likeButtonTapped), for: .touchUpInside)
    shareButton.addTarget(self, action: #selector(shareButtonTapped), for: .touchUpInside)
    deleteButton.addTarget(self, action: #selector(deleteButtonTapped), for: .touchUpInside)
    reportButton.addTarget(self, action: #selector(reportButtonTapped), for: .touchUpInside)
  }

  private func addSignupOverlay() {
    let overlayView = signupOverlayHost.view
    overlayView?.backgroundColor = .clear
    overlayView?.translatesAutoresizingMaskIntoConstraints = false

    contentView.addSubview(overlayView!)

    NSLayoutConstraint.activate([
      overlayView!.topAnchor.constraint(equalTo: contentView.topAnchor),
      overlayView!.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
      overlayView!.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
      overlayView!.trailingAnchor.constraint(equalTo: contentView.trailingAnchor)
    ])
    overlayView?.isHidden = true
  }

  @objc private func handleFirstFrameReady(_ note: Notification) {
    guard let idx = note.userInfo?["index"] as? Int, idx == index else { return }
    playerLayer?.isHidden = false
  }

  @objc func likeButtonTapped() {
    delegate?.likeButtonTapped(index: index)
  }

  @objc func shareButtonTapped() {
    delegate?.shareButtonTapped(index: index)
  }

  @objc func deleteButtonTapped() {
    delegate?.deleteButtonTapped(index: index)
  }

  @objc func reportButtonTapped() {
    delegate?.reportButtonTapped(index: index)
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
      playerContainerView.image = Constants.playerPlaceHolderImage
    }

    playerLayer?.removeFromSuperlayer()
    playerLayer?.player = nil
    playerLayer = nil
    let layer = AVPlayerLayer(player: player)
    layer.isHidden = true
    layer.videoGravity = .resize
    playerContainerView.layer.addSublayer(layer)
    playerLayer = layer
    playerLayer?.frame = contentView.bounds

    likeButton.configuration?.attributedTitle = AttributedString(
      String(feedInfo.likeCount),
      attributes: AttributeContainer([
        .font: Constants.actionButtonFont
      ])
    )
    likeButton.configuration?.image = feedInfo.isLiked ? Constants.likeSelectedImage : Constants.likeUnSelectedImage

    self.index = index
    self.feedType = feedInfo.feedType

    if feedInfo.feedType == .otherUsers {
      profileInfoView.set(data: profileInfo)
      profileInfoView.isHidden = false
      captionScrollView.isHidden = true
      actionsStackView.addArrangedSubview(reportButton)
      deleteButton.removeFromSuperview()
    } else {
      reportButton.removeFromSuperview()
      actionsStackView.addArrangedSubview(deleteButton)
      profileInfoView.isHidden = true
      captionScrollView.isHidden = false
      setCaptionHeight(captionText: profileInfo.subtitle)
    }
    signupOverlayHost.view.isHidden = !feedInfo.showLoginOverlay
  }

  func setLikeStatus(isLiked: Bool) {
    likeButton.configuration?.image = isLiked ? Constants.likeSelectedImage : Constants.likeUnSelectedImage
    var likeButtonString = String((Int(likeButton.titleLabel?.text ?? "") ?? .zero) - .one)
    if isLiked {
      likeButtonString = String((Int(likeButton.titleLabel?.text ?? "") ?? Int.zero) + Int.one)
    }
    likeButton.configuration?.attributedTitle = AttributedString(
      likeButtonString,
      attributes: AttributeContainer([
        .font: Constants.actionButtonFont
      ])
    )
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    playerLayer?.frame = playerContainerView.bounds
  }

  override func prepareForReuse() {
    super.prepareForReuse()
    playerLayer?.player = nil
    playerLayer?.removeFromSuperlayer()
    playerLayer = nil
    playerContainerView.sd_cancelCurrentImageLoad()
    playerContainerView.image = nil
  }

  struct FeedCellInfo {
    let thumbnailURL: URL?
    let likeCount: Int
    let isLiked: Bool
    let lastThumbnailImage: UIImage?
    let feedType: FeedType
    let showLoginOverlay: Bool
  }
}

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 14.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 116.0
    static let stackViewBottom = 74.0
    static let stackViewBGColor = UIColor.clear
    static let actionButtonFont = YralFont.pt16.semiBold.uiFont
    static let likeSelectedImage = UIImage(named: "like_selected_feed")
    static let likeUnSelectedImage = UIImage(named: "like_unselected_feed")
    static let shareButtonImage = UIImage(named: "share_feed")
    static let deleteButtonImage = UIImage(named: "delete_video_profile")
    static let reportButtonImage = UIImage(named: "report_feed")
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
    static let captionTitleFont = YralFont.pt14.regular.uiFont
    static let captionTextColor = YralColor.grey200.uiColor
    static let captionsBottomMargin = 40.0
    static let captionSingleLineHeight: CGFloat = 20
    static let maxLinesCaption = 10.0
    static let animationPeriod = 0.3
  }
}
// swiftlint: enable type_body_length
