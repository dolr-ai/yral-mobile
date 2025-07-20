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
import iosSharedUmbrella

protocol FeedsCellProtocol: AnyObject {
  func shareButtonTapped(index: Int)
  func deleteButtonTapped(index: Int)
  func reportButtonTapped(index: Int)
  func loginTapped(provider: SocialProvider)
  func smileyTapped(index: Int, smiley: Smiley)
  func showGameResultBottomSheet(index: Int, gameResult: SmileyGameResultResponse)
  func videoStarted(index: Int, videoId: String)
  func howToPlayButtonTapped(index: Int)
  func accountButtonTapped(index: Int)
  func gameToggleTapped(index: Int, gameIndex: Int)
}

// swiftlint: disable type_body_length
class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  var index: Int = .zero
  var feedType: FeedType = .otherUsers
  weak var delegate: FeedsCellProtocol?
  private let userDefaults = UserDefaults.standard
  private static let resultBottomSheetKey = "ResultBottomSheetKey"
  private var cancellables = Set<AnyCancellable>()
  private var smileyGame: SmileyGame?
  private var honGame: HonGame?
  private var activeGame: FeedGame?
  private var sessionManager: SessionManager? {
    didSet {
      bindSession()
    }
  }

  private var showResultBottomSheet: Bool {
    userDefaults.integer(forKey: Self.resultBottomSheetKey) < Int.one ? true : false
  }

  private let playerContainerView = getUIImageView()
  private lazy var signupOverlayHost = UIHostingController(
    rootView: SignupOverlay { provider in
      self.delegate?.loginTapped(provider: provider)
    }
  )

  private let topGradientImage: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.topGradientImage
    imageView.contentMode = .scaleToFill
    imageView.clipsToBounds = true
    return imageView
  }()

  private let bottomGradientImage: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = Constants.bottomGradientImage
    imageView.contentMode = .scaleToFill
    imageView.clipsToBounds = true
    return imageView
  }()

  var actionsStackView: UIStackView = {
    let stackView = getUIStackView()
    stackView.axis = .vertical
    stackView.distribution = .fill
    stackView.spacing = Constants.stackViewSpacing
    stackView.backgroundColor = Constants.stackViewBGColor
    stackView.alignment = .trailing
    return stackView
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

  private var accountImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.widthAnchor.constraint(equalToConstant: Constants.accountImageSize).isActive = true
    imageView.heightAnchor.constraint(equalToConstant: Constants.accountImageSize).isActive = true
    imageView.layer.cornerRadius = Constants.accountImageSize / 2
    imageView.clipsToBounds = true
    imageView.layer.borderWidth = Constants.accountImageBorderWidth
    imageView.layer.borderColor = Constants.accountImageBorderColor
    return imageView
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

  var smileyGameHostController: UIHostingController<SmileyGameView>?

  let lottieView: LottieAnimationView = {
    let view = LottieAnimationView()
    view.translatesAutoresizingMaskIntoConstraints = false
    view.contentMode = .scaleAspectFit
    return view
  }()

  let smileyGameScoreLabel: UILabel = {
    let label = getUILabel()
    label.font = YralFont.pt64.bold.uiFont
    return label
  }()

  lazy var howToPlayButtonView: UIView = {
    let buttonView = FeedsCell.getUIView()
    buttonView.layer.cornerRadius = Constants.howToPlayHeight / CGFloat.two
    buttonView.backgroundColor = Constants.howToPlayBackgroundColor
    buttonView.isUserInteractionEnabled = true
    let tapGesture = UITapGestureRecognizer(target: self, action: #selector(howToPlayButtonTapped))
    buttonView.addGestureRecognizer(tapGesture)
    return buttonView
  }()

  let howToPlayImageBackground: UIView = {
    let view = getUIView()
    view.backgroundColor = Constants.howToPlayImageBackground
    view.layer.cornerRadius = Constants.howToPlayImageBackgroundSize / CGFloat.two
    return view
  }()

  let howToPlayImageView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = UIImage(named: Constants.howToPlayImage)
    imageView.layer.cornerRadius = Constants.howToPlayImageSize / CGFloat.two
    return imageView
  }()

  let howToPlayLabel: UILabel = {
    let label = getUILabel()
    label.text = Constants.howToPlayText
    label.font = Constants.howToPlayTextFont
    label.textColor = Constants.howToPlayTextColor
    return label
  }()

  var isCaptionExpanded = false
  var collapsedCaptionHeight: CGFloat = 0
  var expandedCaptionHeight: CGFloat = 0
  var isCaptionCollapsible = false
  var captionScrollViewHeightConstraint: NSLayoutConstraint!
  var howToPlayWidthConstraint: NSLayoutConstraint!

  let resultAnimationPublisher = PassthroughSubject<SmileyGameResultResponse, Never>()
  let initialStatePublisher = PassthroughSubject<SmileyGame, Never>()
  let smileyGameErrorPublisher = PassthroughSubject<String, Never>()

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

  private lazy var headerView: HeaderView = {
    let headerView = HeaderView()
    headerView.heightAnchor.constraint(equalToConstant: Constants.headerViewHeight).isActive = true
    headerView.delegate = self
    return headerView
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
    setupGradientImages()
    setupHeaderView()
    setupStackView()
    setupCaptionLabel()
    addSignupOverlay()
    setupHowToPlayView()

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

  private func setupGradientImages() {
    contentView.addSubview(topGradientImage)
    contentView.addSubview(bottomGradientImage)

    NSLayoutConstraint.activate([
      topGradientImage.leadingAnchor.constraint(equalTo: leadingAnchor),
      topGradientImage.trailingAnchor.constraint(equalTo: trailingAnchor),
      topGradientImage.topAnchor.constraint(equalTo: topAnchor),
      topGradientImage.heightAnchor.constraint(equalToConstant: Constants.topGradientImageHeight),

      bottomGradientImage.leadingAnchor.constraint(equalTo: leadingAnchor),
      bottomGradientImage.trailingAnchor.constraint(equalTo: trailingAnchor),
      bottomGradientImage.bottomAnchor.constraint(equalTo: bottomAnchor),
      bottomGradientImage.heightAnchor.constraint(equalToConstant: Constants.bottomGradientImageHeight)
    ])
  }

  func setupHeaderView() {
    contentView.addSubview(headerView)
    NSLayoutConstraint.activate([
      headerView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.headerViewHorizontal
      ),
      headerView.trailingAnchor.constraint(
        equalTo: contentView.trailingAnchor,
        constant: -Constants.headerViewHorizontal
      ),
      headerView.topAnchor.constraint(
        equalTo: contentView.topAnchor,
        constant: Constants.headerViewTop
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
    guard let idx = note.userInfo?["index"] as? Int, idx == index,
          let videoId = note.userInfo?["videoId"] as? String else { return }
    playerLayer?.isHidden = false
    delegate?.videoStarted(index: index, videoId: videoId)
  }

  private func bindSession() {
    cancellables.forEach { $0.cancel() }
    cancellables.removeAll()

    sessionManager?.$state
      .receive(on: RunLoop.main)
      .compactMap { state -> (coins: UInt64, fetching: Bool)? in
        switch state {
        case .ephemeralAuthentication(_, _, let coins, let fetching),
            .permanentAuthentication(_, _, let coins, let fetching):
          return (coins, fetching)
        default:
          return nil
        }
      }
      .filter { !$0.fetching }
      .map(\.coins)
      .removeDuplicates()
      .sink { [weak self] coins in
        self?.headerView.coinsView.coins = coins
      }
      .store(in: &cancellables)
  }

  private func setupHowToPlayView() {
    contentView.addSubview(howToPlayButtonView)
    howToPlayButtonView.addSubview(howToPlayImageBackground)
    howToPlayButtonView.addSubview(howToPlayLabel)
    howToPlayImageBackground.addSubview(howToPlayImageView)

    howToPlayWidthConstraint = howToPlayButtonView.widthAnchor.constraint(equalToConstant: Constants.howToPlayMinWidth)
    howToPlayWidthConstraint.isActive = true
    howToPlayLabel.isHidden = true

    NSLayoutConstraint.activate([
      howToPlayButtonView.leadingAnchor.constraint(
        equalTo: contentView.leadingAnchor,
        constant: Constants.howToPlayLeading
      ),
      howToPlayButtonView.heightAnchor.constraint(equalToConstant: Constants.howToPlayHeight),
      howToPlayButtonView.bottomAnchor.constraint(
        equalTo: contentView.bottomAnchor,
        constant: -Constants.howToPlayBottom
      ),

      howToPlayImageBackground.leadingAnchor.constraint(
        equalTo: howToPlayButtonView.leadingAnchor,
        constant: .zero
      ),
      howToPlayImageBackground.widthAnchor.constraint(equalToConstant: Constants.howToPlayImageBackgroundSize),
      howToPlayImageBackground.heightAnchor.constraint(equalToConstant: Constants.howToPlayImageBackgroundSize),
      howToPlayImageBackground.centerYAnchor.constraint(equalTo: howToPlayButtonView.centerYAnchor),

      howToPlayImageView.widthAnchor.constraint(equalToConstant: Constants.howToPlayImageSize),
      howToPlayImageView.heightAnchor.constraint(equalToConstant: Constants.howToPlayImageSize),
      howToPlayImageView.centerXAnchor.constraint(equalTo: howToPlayImageBackground.centerXAnchor),
      howToPlayImageView.centerYAnchor.constraint(equalTo: howToPlayImageBackground.centerYAnchor),

      howToPlayLabel.leadingAnchor.constraint(
        equalTo: howToPlayImageBackground.trailingAnchor,
        constant: Constants.howToPlayTextLeading
      ),
      howToPlayLabel.centerYAnchor.constraint(equalTo: howToPlayButtonView.centerYAnchor)
    ])
  }

  func setupGameView() {
    switch activeGame {
    case .smiley:
      if let smileyGame = smileyGame {
        setupSmileyGameView(smileyGame: smileyGame)
        headerView.addGameToggleView(with: .one)
      }
    case .hon:
      if let honGame = honGame {
        setupHONGameView(honGame: honGame)
        headerView.addGameToggleView(with: .zero)
      }
    default:
      break
    }
  }

  func setupSmileyGameView(smileyGame: SmileyGame) {
    guard (sessionManager?.state.coins ?? 0) >= SmileyGameConfig.shared.config.lossPenalty else {
      return
    }

    if SmileyGameConfig.shared.config.smileys.count > 0 {
      let smileyGameView = SmileyGameView(
        smileyGame: SmileyGame(
          config: SmileyGameConfig.shared.config,
          state: smileyGame.state
        ),
        smileyTapped: { [weak self] smiley in
          self?.handleSmileyTap(smiley)
        },
        resultAnimationSubscriber: resultAnimationPublisher,
        initialStateSubscriber: initialStatePublisher,
        errorSubscriber: smileyGameErrorPublisher
      )

      let controller = UIHostingController(rootView: smileyGameView)
      controller.view.backgroundColor = .clear
      controller.view.translatesAutoresizingMaskIntoConstraints = false

      contentView.addSubview(controller.view)

      if let overlayView = signupOverlayHost.view {
        contentView.bringSubviewToFront(overlayView)
      }

      NSLayoutConstraint.activate([
        controller.view.leadingAnchor.constraint(
          equalTo: contentView.leadingAnchor,
          constant: Constants.smileyGameHorizontal
        ),
        controller.view.trailingAnchor.constraint(
          equalTo: contentView.trailingAnchor,
          constant: -Constants.smileyGameHorizontal
        ),
        controller.view.bottomAnchor.constraint(
          equalTo: contentView.bottomAnchor,
          constant: -Constants.smileyGameBottom
        ),
        controller.view.heightAnchor.constraint(
          equalToConstant: Constants.smileyGameHeight
        )
      ])

      smileyGameHostController = controller
    }
  }

  func setupHONGameView(honGame: HonGame) {

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

  private func handleSmileyTap(_ smiley: Smiley) {
    delegate?.smileyTapped(index: index, smiley: smiley)
    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.smileyTapDuration) {
      self.startFlowingAnimation(for: smiley)
    }
  }

  private func startFlowingAnimation(for smiley: Smiley) {
    let animation = LottieAnimation.named("smiley_game_\(smiley.id)")
    lottieView.animation = animation
    lottieView.play()
  }

  func startSmileyGamResultAnimation(for result: SmileyGameResultResponse, completion: @escaping () -> Void) {
    AudioPlayer.shared.play(named: result.outcome == "WIN" ? Constants.winSound : Constants.lossSound)
    headerView.coinsView.updateCoins(by: result.coinDelta)
    if showResultBottomSheet {
      let existingCount = userDefaults.integer(forKey: Self.resultBottomSheetKey)
      userDefaults.set(existingCount + Int.one, forKey: Self.resultBottomSheetKey)
      userDefaults.synchronize()
      delegate?.showGameResultBottomSheet(index: index, gameResult: result)
    } else {
      if result.outcome == "WIN" {
        smileyGameScoreLabel.text = "+\(result.coinDelta)"
        smileyGameScoreLabel.textColor = Constants.scoreLabelWinColor
      } else {
        smileyGameScoreLabel.text = "\(result.coinDelta)"
        smileyGameScoreLabel.textColor = Constants.scoreLabelLooseColor
      }
    }

    if !showResultBottomSheet {
      startScoreLabelAnimation(for: result)
      DispatchQueue.main.asyncAfter(deadline: .now() + Constants.resultAnimationDuration) {
        completion()
      }
    } else {
      resultAnimationPublisher.send(result)
      DispatchQueue.main.asyncAfter(deadline: .now() + Constants.resultAnimationDurationWithBS) {
        completion()
      }
    }
  }

  private func startScoreLabelAnimation(for result: SmileyGameResultResponse) {
    smileyGameScoreLabel.isHidden = false
    UIView.animate(withDuration: Constants.scoreLabelDuration) {
      self.smileyGameScoreLabel.transform = CGAffineTransform(translationX: .zero, y: -UIScreen.main.bounds.height)
    } completion: { complete in
      if complete {
        self.smileyGameScoreLabel.isHidden = true
        self.smileyGameScoreLabel.transform = .identity
        self.resultAnimationPublisher.send(result)
      }
    }
  }

  func handleSmileyGameError(_ errorMessage: String) {
    smileyGameErrorPublisher.send(errorMessage)
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

  @objc func howToPlayButtonTapped() {
    delegate?.howToPlayButtonTapped(index: index)
  }

  // swiftlint: disable function_parameter_count
  // swiftlint: disable function_body_length
  func configure(
    withPlayer feedsPlayer: FeedsPlayer,
    feedInfo: FeedCellInfo,
    profileInfo: HeaderView.ProfileInfo,
    smileyGame: SmileyGame?,
    honGame: HonGame?,
    activeGame: FeedGame?,
    session: SessionManager,
    index: Int
  ) {
    playerContainerView.sd_cancelCurrentImageLoad()
    playerContainerView.image = Constants.playerPlaceHolderImage

    if let lastThumbnailImage = feedInfo.lastThumbnailImage {
      playerContainerView.image = lastThumbnailImage
    } else if let thumbnailURL = feedInfo.thumbnailURL {
      loadImage(with: thumbnailURL, placeholderImage: Constants.playerPlaceHolderImage, on: playerContainerView)
    }

    playerLayer?.removeFromSuperlayer()
    playerLayer?.player = nil
    playerLayer = nil
    guard let player = feedsPlayer.player as? AVQueuePlayer else { return }
    let layer = AVPlayerLayer(player: player)
    layer.videoGravity = .resize

    let isCurrentReel = index == feedsPlayer.currentIndex
    let itemReady = player.currentItem?.status == .readyToPlay
    let alreadyPlaying = player.timeControlStatus == .playing

    layer.isHidden = !(isCurrentReel && alreadyPlaying && itemReady)

    playerContainerView.layer.addSublayer(layer)
    playerLayer = layer
    playerLayer?.frame = contentView.bounds

    self.index = index
    self.feedType = feedInfo.feedType
    self.smileyGame = smileyGame
    self.honGame = honGame
    self.activeGame = activeGame
    self.sessionManager = session

    actionsStackView.addArrangedSubview(accountImageView)

    if let imageURL = profileInfo.imageURL {
      loadImage(with: imageURL, placeholderImage: nil, on: accountImageView)
    } else {
      accountImageView.image = Constants.defaultProfileImage
    }

    if feedInfo.feedType == .otherUsers {
      headerView.set(coins: profileInfo.coins)
      headerView.isHidden = false
      captionScrollView.isHidden = true
      actionsStackView.addArrangedSubview(reportButton)
      deleteButton.removeFromSuperview()
      setupGameView()
    } else {
      reportButton.removeFromSuperview()
      actionsStackView.addArrangedSubview(deleteButton)
      headerView.isHidden = true
      captionScrollView.isHidden = false
      setCaptionHeight(captionText: profileInfo.subtitle)
    }
    signupOverlayHost.view.isHidden = !feedInfo.showLoginOverlay
    if feedInfo.showLoginOverlay {
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: AuthScreenViewedEventData()
      )

    }
  }
  // swiftlint: enable function_body_length
  // swiftlint: enable function_parameter_count

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

    headerView.coinsView.resetUIState()
    lottieView.stop()
    smileyGameScoreLabel.transform = .identity
    smileyGameScoreLabel.isHidden = true

    if let smileyGame = smileyGame {
      initialStatePublisher.send(smileyGame)
    }

    smileyGameHostController?.view.removeFromSuperview()
    smileyGameHostController = nil

    headerView.gameToggleController?.view.removeFromSuperview()
    headerView.gameToggleController = nil

    cancellables.forEach({ $0.cancel() })
    cancellables.removeAll()
  }

  func startListeningForFirstFrame() {
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleFirstFrameReady(_:)),
      name: .feedItemReady,
      object: nil)

  }

  func stopListeningForFirstFrame() {
    NotificationCenter.default.removeObserver(
      self,
      name: .feedItemReady,
      object: nil
    )
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
// swiftlint: enable type_body_length

extension FeedsCell: HeaderViewDelegate {
  func didTapAccountButton() {
    delegate?.accountButtonTapped(index: index)
  }

  func didTapGameToggle(index: Int) {
    delegate?.gameToggleTapped(index: self.index, gameIndex: index)

    DispatchQueue.main.asyncAfter(deadline: .now() + Constants.gameToggleTransitionDuration) {
      if index == .zero {
        self.activeGame = .hon
        self.smileyGameHostController?.view.removeFromSuperview()
        self.smileyGameHostController = nil
      } else if index == .one {
        self.activeGame = .smiley
      }

      self.headerView.gameToggleController?.view.removeFromSuperview()
      self.headerView.gameToggleController = nil
      self.setupGameView()
    }
  }
}

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 26.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 98.0
    static let stackViewBottom = 90.0
    static let stackViewBGColor = UIColor.clear
    static let actionButtonFont = YralFont.pt16.semiBold.uiFont
    static let shareButtonImage = UIImage(named: "share_feed")
    static let deleteButtonImage = UIImage(named: "delete_video_profile")
    static let reportButtonImage = UIImage(named: "report_feed")
    static let actionButtonHeight: CGFloat = 36.0
    static let actionButtonWidth: CGFloat = 36.0
    static let actionButtonImagePadding = 4.0
    static let actionButtonTitleColor = YralColor.grey50.uiColor
    static let headerViewHorizontal = 26.0
    static let headerViewTop = 12.0
    static let headerViewHeight = 50.0
    static let defaultProfileImage = UIImage(named: "default_profile")
    static let playerPlaceHolderImage = UIImage(named: "player_placeholder")
    static let captionTitleFont = YralFont.pt14.regular.uiFont
    static let captionTextColor = YralColor.grey200.uiColor
    static let captionsBottomMargin = 40.0
    static let captionSingleLineHeight: CGFloat = 20
    static let maxLinesCaption = 10.0
    static let animationPeriod = 0.3
    static let smileyGameHorizontal = 20.0
    static let smileyGameBottom = 16.0
    static let smileyGameHeight = 64.0
    static let smileyTapDuration = 0.2
    static let winSound = "smiley_game_win"
    static let lossSound = "smiley_game_loss"
    static let scoreLabelWinColor = YralColor.green300.uiColor.withAlphaComponent(0.3)
    static let scoreLabelLooseColor = YralColor.red300.uiColor.withAlphaComponent(0.3)
    static let resultAnimationDuration = 2.5
    static let resultAnimationDurationWithBS = 0.5
    static let scoreLabelDuration = 2.0

    static let accountImageSize = 36.0
    static let accountImageBorderWidth = 2.5
    static let accountImageBorderColor = YralColor.grey50.uiColor.cgColor

    static let howToPlayHeight = 44.0
    static let howToPlayWidth = 126.0
    static let howToPlayMinWidth = 44.0
    static let howToPlayLeading = 20.0
    static let howToPlayBottom = 90.0
    static let howToPlayBackgroundColor = UIColor.black.withAlphaComponent(0.4)
    static let howToPlayText = "How to Play"
    static let howToPlayTextFont = YralFont.pt12.semiBold.uiFont
    static let howToPlayTextColor = YralColor.grey0.uiColor
    static let howToPlayTextLeading = 2.0
    static let howToPlayImage = "how_to_play"
    static let howToPlayImageSize = 32.0
    static let howToPlayImageBackground = UIColor.black.withAlphaComponent(0.2)
    static let howToPlayImageBackgroundSize = 44.0

    static let topGradientImage = UIImage(named: "feed_top_gradient")
    static let bottomGradientImage = UIImage(named: "feed_bottom_gradient")
    static let topGradientImageHeight = 150.0
    static let bottomGradientImageHeight = 300.0

    static let gameToggleTransitionDuration = 1.5
  }
}
