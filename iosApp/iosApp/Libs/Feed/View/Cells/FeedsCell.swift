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
  func rechargeWallet(index: Int, smiley: Smiley)
  func showGameResultBottomSheet(index: Int, gameResult: SmileyGameResultResponse)
  func videoStarted(index: Int, videoId: String)
  func walletAnimationStarted()
  func walletAnimationEnded(success: Bool, coins: Int64)
  func howToPlayTapped()
  func howToPlayShown(index: Int)
  func coinsTapped()
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
  var smileyGame: SmileyGame?
  var smileyGameView: SmileyGameView?
  private var sessionManager: SessionManager? {
    didSet {
      bindSession()
    }
  }

  private var showResultBottomSheet: Bool {
    userDefaults.integer(forKey: Self.resultBottomSheetKey) < Int.one ? true : false
  }

  private let playerContainerView = getUIImageView()
  lazy var signupOverlayHost = UIHostingController(
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

  var isCaptionExpanded = false
  var collapsedCaptionHeight: CGFloat = 0
  var expandedCaptionHeight: CGFloat = 0
  var isCaptionCollapsible = false
  var captionScrollViewHeightConstraint: NSLayoutConstraint!

  let resultAnimationPublisher = PassthroughSubject<SmileyGameResultResponse, Never>()
  let initialStatePublisher = PassthroughSubject<SmileyGame, Never>()
  let smileyGameErrorPublisher = PassthroughSubject<String, Never>()
  let animatePublisher = PassthroughSubject<SmileyConfig, Never>()
  let animationCompletionPublisher = PassthroughSubject<Void, Never>()

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

  var profileInfoView: ProfileInfoView = {
    let profileInfoView = ProfileInfoView()
    profileInfoView.heightAnchor.constraint(equalToConstant: Constants.profileInfoViewHeight).isActive = true
    return profileInfoView
  }()
  var gameInfoOverlayView = getUIView()
  var onboardingInfoView = OnboardingInfoView()
  var playToScrollInfoView = PlayToScrollInfoView()
  var howToPlayButton: UIButton = {
    let button = getUIButton()
    button.backgroundColor = .clear
    button.setImage(UIImage(named: Constants.howToPlayImageCollapsed), for: .normal)
    button.contentHorizontalAlignment = .left
    button.imageView?.contentMode = .left
    button.contentEdgeInsets = .zero
    button.imageEdgeInsets = .zero
    button.setContentHuggingPriority(.required, for: .horizontal)
    button.setContentCompressionResistancePriority(.required, for: .horizontal)
    return button
  }()
  var howToPlayWidthAnchor: NSLayoutConstraint!
  var starsView: UIImageView = {
    let imageView = getUIImageView()
    imageView.image = UIImage(named: Constants.starsImageName)
    imageView.contentMode = .scaleToFill
    return imageView
  }()
  var expectedVideoID: String?

  enum RechargeResult { case success, failure }

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
    playerContainerView.contentMode = .scaleAspectFill
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
    profileInfoView.delegate = self
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
    guard idx == index, videoId == expectedVideoID else { return }
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
            .permanentAuthentication(_, _, _, let coins, let fetching):
          return (coins, fetching)
        default:
          return nil
        }
      }
      .filter { !$0.fetching }
      .map(\.coins)
      .removeDuplicates()
      .sink { [weak self] coins in
        self?.profileInfoView.coinsView.coins = coins
      }
      .store(in: &cancellables)
  }

  func setupSmileyGameView() {
    if SmileyGameConfig.shared.config.smileys.count > 0 {
      smileyGameView = SmileyGameView(
        smileyGame: SmileyGame(
          config: SmileyGameConfig.shared.config,
          state: smileyGame?.state ?? .notPlayed
        ),
        smileyTapped: { [weak self] smiley in
          self?.handleSmileyTap(smiley)
        },
        resultAnimationSubscriber: resultAnimationPublisher,
        initialStateSubscriber: initialStatePublisher,
        errorSubscriber: smileyGameErrorPublisher,
        animateSubscriber: animatePublisher,
        animationCompletionSubscriber: animationCompletionPublisher
      )

      let controller = UIHostingController(rootView: smileyGameView!)
      controller.view.backgroundColor = .clear
      controller.view.translatesAutoresizingMaskIntoConstraints = false

      contentView.addSubview(controller.view)

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
      addHowToPlay()
      if let overlayView = signupOverlayHost.view {
        contentView.bringSubviewToFront(overlayView)
      }
    }
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
    if let smileyView = smileyGameHostController?.view {
      cleanupOnOnboardingCompletion(smileyView: smileyView)
    }
    if sessionManager?.state.coins ?? UInt64(.zero) >= SmileyGameConfig.shared.config.lossPenalty {
      delegate?.smileyTapped(index: index, smiley: smiley)
      DispatchQueue.main.asyncAfter(deadline: .now() + Constants.smileyTapDuration) {
        self.startFlowingAnimation(for: smiley)
      }
    } else {
      delegate?.rechargeWallet(index: index, smiley: smiley)
      beginWalletRechargeLoading()
    }
    UserDefaultsManager.shared.set(true, for: .onboardingCompleted)
  }

  private func startFlowingAnimation(for smiley: Smiley) {
    var lottieAnimation: LottieAnimation?

    FirebaseLottieManager.shared.data(forPath: smiley.clickAnimation) { result in
      switch result {
      case .success(let lottieData):
        lottieAnimation = try? LottieAnimation.from(data: lottieData)
      case .failure:
        lottieAnimation = LottieAnimation.named("smiley_game_\(smiley.id)")
      }
    }

    lottieView.isHidden = false
    lottieView.animation = lottieAnimation
    lottieView.backgroundColor = .clear
    lottieView.play()
  }

  func startSmileyGamResultAnimation(for result: SmileyGameResultResponse, completion: @escaping () -> Void) {
    self.smileyGame?.state = .played(result)
    HapticGenerator.performFeedback(.impact(weight: .medium))
    AudioPlayer.shared.play(named: result.outcome == "WIN" ? Constants.winSound : Constants.lossSound)
    profileInfoView.coinsView.updateCoins(by: result.coinDelta)
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

  // swiftlint: disable function_parameter_count
  func configure(
    withPlayer feedsPlayer: FeedsPlayer?,
    feedInfo: FeedCellInfo,
    profileInfo: ProfileInfoView.ProfileInfo,
    smileyGame: SmileyGame?,
    session: SessionManager,
    index: Int
  ) {
    if let thumbnailURL = feedInfo.thumbnailURL {
      loadImage(with: thumbnailURL, placeholderImage: Constants.playerPlaceHolderImage, on: playerContainerView)
    }
    self.expectedVideoID = feedsPlayer?.feedResults[safe: index]?.videoID

    if let player = feedsPlayer?.player as? AVQueuePlayer {
      let layer = AVPlayerLayer(player: player)
      layer.videoGravity = .resizeAspectFill

      layer.isHidden = true
      playerContainerView.layer.addSublayer(layer)
      playerLayer = layer
      playerLayer?.frame = contentView.bounds
    }

    self.index = index
    self.feedType = feedInfo.feedType
    self.smileyGame = smileyGame
    self.sessionManager = session

    if feedInfo.feedType == .otherUsers {
      profileInfoView.set(data: profileInfo)
      profileInfoView.isHidden = false
      captionScrollView.isHidden = true
      actionsStackView.addArrangedSubview(shareButton)
      actionsStackView.addArrangedSubview(reportButton)
      deleteButton.removeFromSuperview()
      setupSmileyGameView()
      if feedInfo.showOnboarding, feedsPlayer != nil {
        showOnboardingFlow()
      }
    } else {
      reportButton.removeFromSuperview()
      actionsStackView.addArrangedSubview(shareButton)
      actionsStackView.addArrangedSubview(deleteButton)
      profileInfoView.isHidden = true
      captionScrollView.isHidden = false
      setCaptionHeight(captionText: profileInfo.subtitle)
    }
    signupOverlayHost.view.isHidden = !feedInfo.showLoginOverlay
    if feedInfo.showLoginOverlay {
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: AuthScreenViewedEventData(pageName: .home)
      )
    }
  }
  // swiftlint: enable function_parameter_count

  override func layoutSubviews() {
    super.layoutSubviews()
    playerLayer?.frame = playerContainerView.bounds
  }

  override func prepareForReuse() {
    super.prepareForReuse()
    playerLayer?.isHidden = true
    playerLayer?.player = nil
    playerLayer?.removeFromSuperlayer()
    playerLayer = nil
    playerContainerView.image = nil

    profileInfoView.coinsView.resetUIState()
    lottieView.stop()
    smileyGameScoreLabel.transform = .identity
    smileyGameScoreLabel.isHidden = true
    if let game = smileyGame {
      initialStatePublisher.send(game)
    }
    guard let smileyView = smileyGameHostController?.view else { return }
    cleanupOnOnboardingCompletion(smileyView: smileyView)
    smileyGameHostController?.view.removeFromSuperview()
    smileyGameHostController = nil

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
    let feedType: FeedType
    let showLoginOverlay: Bool
    let showOnboarding: Bool
  }
}
// swiftlint: enable type_body_length

extension FeedsCell {
  enum Constants {
    static let stackViewSpacing = 32.0
    static let horizontalMargin = 16.0
    static let stackViewHeight = 104.0
    static let stackViewBottom = 96.0
    static let stackViewBGColor = UIColor.clear
    static let actionButtonFont = YralFont.pt16.semiBold.uiFont
    static let shareButtonImage = UIImage(named: "share_feed")
    static let deleteButtonImage = UIImage(named: "delete_video_profile")
    static let reportButtonImage = UIImage(named: "report_feed")
    static let actionButtonHeight: CGFloat = 36.0
    static let actionButtonWidth: CGFloat = 36.0
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
    static let onboardingBGColor = UIColor.black.withAlphaComponent(0.8)
    static let smileyGameShadowColor = YralColor.grey0.uiColor.withAlphaComponent(0.6).cgColor
    static let smileyShadowMaxRadius = 21.5
    static let onbaordingInfoViewHeight: CGFloat = 260.0
    static let onbaordingInfoViewBottom: CGFloat = -36.0
    static let onbaordingInfoViewHorizontalSpacing: CGFloat = 24.0
    static let onboardingInfoAnimationTranslation: CGFloat = 10.0
    static let howToPlayImageCollapsed = "howtoplay_collapsed"
    static let howToPlayImageExpanded = "howtoplay_expanded"
    static let howToPlayHeight = 42.0
    static let howToPlayCollapsedWidth = 32.0
    static let howToPlayExpandedWidth = 122.0
    static let starsImageName = "stars_onboarding_bg"
    static let starsImageViewHeight = 130.0
    static let starsImageTop = 44.0
    static let howToPlayInfoViewHeight: CGFloat = 214.0
  }
}
