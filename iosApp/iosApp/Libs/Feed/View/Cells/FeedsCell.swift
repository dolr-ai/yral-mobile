//
//  FeedsCell.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

// swiftlint: disable file_length
import UIKit
import SwiftUI
import AVFoundation
import Lottie
import Combine

protocol FeedsCellProtocol: AnyObject {
  func shareButtonTapped(index: Int)
  func likeButtonTapped(index: Int)
  func deleteButtonTapped(index: Int)
  func reportButtonTapped(index: Int)
  func smileyTapped(index: Int, smiley: Smiley)
  func showGameResultBottomSheet(index: Int, gameResult: SmileyGameResult)
}
// swiftlint: disable type_body_length
class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  var index: Int = .zero
  var feedType: FeedType = .otherUsers
  weak var delegate: FeedsCellProtocol?
  private let userDefaults = UserDefaults.standard
  private static let resultBottomSheetKey = "ResultBottomSheetKey"

  private var showResultBottomSheet: Bool {
    userDefaults.integer(forKey: Self.resultBottomSheetKey) < 3 ? true : false
  }

  private let playerContainerView = getUIImageView()

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

  lazy var smileyGameView: UIView = {
    let view = UIHostingController(rootView:
                                    SmileyGameView(smileyTapped: { [weak self] smiley in
      self?.handleSmileyTap(smiley)
    },
                                                   resultAnimationSubscriber: resultAnimationPublisher,
                                                   initialStateSubscriber: initialStatePublisher
                                                  ))
    view.view.translatesAutoresizingMaskIntoConstraints = false
    view.view.backgroundColor = .clear
    return view.view
  }()

  let lottieView: LottieAnimationView = {
    let view = LottieAnimationView()
    view.translatesAutoresizingMaskIntoConstraints = false
    view.contentMode = .scaleAspectFit
    view.loopMode = .playOnce
    view.animationSpeed = 1.0
    return view
  }()

  let smileyGameScoreLabel: UILabel = {
    let label = getUILabel()
    label.font = YralFont.pt64.bold.uiFont
    return label
  }()

  var isCaptionExpanded = false
  var collapsedCaptionHeight: CGFloat = 0
  var expandedCaptionHeight: CGFloat = 0
  var isCaptionCollapsible = false
  var captionScrollViewHeightConstraint: NSLayoutConstraint!

  let resultAnimationPublisher = PassthroughSubject<SmileyGameResult, Never>()
  let initialStatePublisher = PassthroughSubject<SmileyGameResult?, Never>()

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
    setupLottieView()
    setupProfileInfoView()
    setupStackView()
    setupCaptionLabel()
    setupSmileyGameView()

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
        constant: -24
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

  private func setupCaptionLabel() {
    contentView.addSubview(captionScrollView)
    captionScrollView.addSubview(captionLabel)

    captionScrollViewHeightConstraint = captionScrollView.heightAnchor
      .constraint(equalToConstant: Constants.captionSingleLineHeight)
    captionScrollViewHeightConstraint.isActive = true

    NSLayoutConstraint.activate([
      captionScrollView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.horizontalMargin
      ),
      captionScrollView.trailingAnchor.constraint(
        equalTo: actionsStackView.leadingAnchor,
        constant: -Constants.horizontalMargin
      ),
      captionScrollView.bottomAnchor.constraint(
        equalTo: contentView.bottomAnchor,
        constant: -Constants.captionsBottomMargin
      )
    ])

    NSLayoutConstraint.activate([
      captionLabel.topAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.topAnchor),
      captionLabel.leadingAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.leadingAnchor),
      captionLabel.trailingAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.trailingAnchor),
      captionLabel.bottomAnchor.constraint(equalTo: captionScrollView.contentLayoutGuide.bottomAnchor),
      captionLabel.widthAnchor.constraint(equalTo: captionScrollView.frameLayoutGuide.widthAnchor)
    ])

    let captionTapGesture = UITapGestureRecognizer(target: self, action: #selector(handleCaptionTap))
    captionScrollView.addGestureRecognizer(captionTapGesture)
  }

  private func setupLottieView() {
    contentView.addSubview(lottieView)
    contentView.addSubview(smileyGameScoreLabel)
    smileyGameScoreLabel.isHidden = true

    NSLayoutConstraint.activate([
      lottieView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
      lottieView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
      lottieView.topAnchor.constraint(equalTo: contentView.topAnchor),
      lottieView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),

      smileyGameScoreLabel.topAnchor.constraint(equalTo: contentView.bottomAnchor),
      smileyGameScoreLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor)
    ])
  }

  private func setupSmileyGameView() {
    contentView.addSubview(smileyGameView)
    NSLayoutConstraint.activate([
      smileyGameView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
      smileyGameView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20),
      smileyGameView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16),
      smileyGameView.heightAnchor.constraint(equalToConstant: 64)
    ])
  }

  private func handleSmileyTap(_ smiley: Smiley) {
    delegate?.smileyTapped(index: index, smiley: smiley)
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
      self.startFlowingAnimation(for: smiley)
    }
  }

  private func startFlowingAnimation(for smiley: Smiley) {
    let animation = LottieAnimation.named("Smiley_Game_\(smiley.name)")
    lottieView.animation = animation
    lottieView.play { [weak self] completed in
      if completed {
        // This animation will run after data is received from API
        let result: SmileyGameResult = Int.random(in: 0...100) % 2 == 0 ? .winner(smiley, 30) : .looser(smiley, -10)
        self?.startSmileyGamResultAnimation(for: result)
      }
    }
  }

  private func startSmileyGamResultAnimation(for result: SmileyGameResult) {
    switch result {
    case .winner(_, let coinsWon):
      profileInfoView.coinsView.updateCoins(by: coinsWon)
      if showResultBottomSheet {
        let existingCount = userDefaults.integer(forKey: Self.resultBottomSheetKey)
        userDefaults.set(existingCount + 1, forKey: Self.resultBottomSheetKey)
        userDefaults.synchronize()
        delegate?.showGameResultBottomSheet(index: index, gameResult: result)
      } else {
        smileyGameScoreLabel.text = "+\(coinsWon)"
        smileyGameScoreLabel.textColor = YralColor.green300.uiColor.withAlphaComponent(0.3)
      }
    case .looser(_, let coinsLoose):
      profileInfoView.coinsView.updateCoins(by: coinsLoose)
      if showResultBottomSheet {
        let existingCount = userDefaults.integer(forKey: Self.resultBottomSheetKey)
        userDefaults.set(existingCount + 1, forKey: Self.resultBottomSheetKey)
        userDefaults.synchronize()
        delegate?.showGameResultBottomSheet(index: index, gameResult: result)
      } else {
        smileyGameScoreLabel.text = "\(coinsLoose)"
        smileyGameScoreLabel.textColor = YralColor.red300.uiColor.withAlphaComponent(0.3)
      }
    }

    if !showResultBottomSheet {
      startScoreLabelAnimation(for: result)
    } else {
      resultAnimationPublisher.send(result)
    }
  }

  private func startScoreLabelAnimation(for result: SmileyGameResult) {
    smileyGameScoreLabel.isHidden = false
    UIView.animate(withDuration: 2) {
      self.smileyGameScoreLabel.transform = CGAffineTransform(translationX: 0, y: -UIScreen.main.bounds.height)
    } completion: { complete in
      if complete {
        self.smileyGameScoreLabel.isHidden = true
        self.smileyGameScoreLabel.transform = .identity
        self.resultAnimationPublisher.send(result)
      }
    }
  }

  @objc private func handleCaptionTap() {
    if !isCaptionExpanded {
      expandCaption()
    } else {
      collapseCaption()
    }
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
      playerContainerView.image = Constants.defaultProfileImage
    }

    playerLayer?.removeFromSuperlayer()
    let layer = AVPlayerLayer(player: player)
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
    profileInfoView.coinsView.resetUIState()
    lottieView.stop()
    smileyGameScoreLabel.transform = .identity
    smileyGameScoreLabel.isHidden = true
    initialStatePublisher.send(nil)
  }

  struct FeedCellInfo {
    let thumbnailURL: URL?
    let likeCount: Int
    let isLiked: Bool
    let lastThumbnailImage: UIImage?
    let feedType: FeedType
  }
}

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 14.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 116.0
    static let stackViewBottom = 90.0
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
    static let profileInfoTrailing = 24.0
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
// swiftlint: enable file_length
