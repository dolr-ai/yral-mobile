//
//  FeedsViewController.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import UIKit
import Combine
import AVFoundation
import SDWebImage
import iosSharedUmbrella

// swiftlint: disable type_body_length file_length
class FeedsViewController: UIViewController {
  typealias DataSource = UICollectionViewDiffableDataSource<Int, FeedResult>
  typealias Snapshot = NSDiffableDataSourceSnapshot<Int, FeedResult>

  let viewModel: any FeedViewModelProtocol
  var feedType: FeedType = .otherUsers
  var initalFeedscancellables: Set<AnyCancellable> = []
  var paginatedFeedscancellables: Set<AnyCancellable> = []
  var trackedVideoIDs: Set<String> = []
  var lastContentOffsetY: CGFloat = 0
  let playToScroll: Bool
  var isShowingPlayToScroll = false
  var pendingAnchor: DeepLinkFeedRequest?
  var hasAppliedInitialSnapshot = false
  var isApplyingSnapshot = false
  var appDIContainer: AppDIContainer?

  lazy var feedsPlayer: YralPlayer = { [unowned self] in
    let monitor = DefaultNetworkMonitor()
    let downloadManager = HLSDownloadManager(
      networkMonitor: monitor,
      fileManager: .default,
      crashReporter: self.crashReporter
    )
    let player = FeedsPlayer(
      hlsDownloadManager: downloadManager,
      networkMonitor: monitor,
      crashReporter: self.crashReporter
    )
    player.delegate = self
    return player
  }()
  var isCurrentlyVisible = true

  var feedsCV: UICollectionView = {
    let collectionView = getUICollectionView()
    collectionView.showsVerticalScrollIndicator = false
    collectionView.showsHorizontalScrollIndicator = false
    collectionView.backgroundColor = .clear
    collectionView.isPagingEnabled = true
    collectionView.scrollsToTop = false
    collectionView.contentInsetAdjustmentBehavior = .never
    return collectionView
  }()

  lazy var activityIndicator: LottieLoaderContainerView = {
    let loader = LottieLoaderContainerView(animationName: Constants.loaderLottie)
    loader.translatesAutoresizingMaskIntoConstraints = false
    return loader
  }()

  lazy var feedsDataSource = getConfiguredDataSource()
  var loadMoreRequestMade: Bool = false
  var shouldShowFooterLoader: Bool = false
  var pageEndReached: Bool = false
  var onBackButtonTap: (() -> Void)?
  var isTutorialVote = false
  weak var walletAnimationDelegate: FeedsViewControllerRechargeDelegate?
  private var loaderCancellables = Set<AnyCancellable>()
  private var authStateCancellables = Set<AnyCancellable>()
  private var coinFetchingCancellables = Set<AnyCancellable>()
  private var deepLinkCancellables = Set<AnyCancellable>()
  var session: SessionManager
  var deepLinkRouter: DeepLinkRouter
  var eventBus: EventBus
  let crashReporter: CrashReporter

  init(
    viewModel: any FeedViewModelProtocol,
    playToScroll: Bool,
    feedType: FeedType = .otherUsers,
    session: SessionManager,
    eventBus: EventBus,
    crashReporter: CrashReporter
  ) {
    self.viewModel = viewModel
    self.playToScroll = playToScroll
    self.feedType = feedType
    self.session = session
    self.deepLinkRouter = DeepLinkRouter.shared
    self.crashReporter = crashReporter
    self.eventBus = eventBus
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    bindViewModel()
    handleEvents()
    setupNavigationBar()
    setupUI()
    startLoadingBindings()
    addAuthChangeListner()
    addDeepLinkListner()
    Task { @MainActor [weak self] in
      guard let self = self else { return }
      await viewModel.fetchFeeds(request: InitialFeedRequest(numResults: Constants.initialNumResults))
    }
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(appDidBecomeActive),
      name: UIApplication.didBecomeActiveNotification,
      object: nil
    )
  }

  override func viewWillDisappear(_ animated: Bool) {
    super.viewWillDisappear(animated)
    isCurrentlyVisible = false
    feedsPlayer.isPlayerVisible = false
    feedsPlayer.pause()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    isCurrentlyVisible = true
    feedsPlayer.isPlayerVisible = true
    guard !feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
    feedsPlayer.play()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    if self.feedType == .otherUsers {
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: HomePageViewedEventData()
      )
    }
  }

  func bindViewModel() {
    viewModel.unifiedStatePublisher
      .receive(on: RunLoop.main)
      .sink { [weak self] state in
        guard let self = self else { return }
        switch state {
        case .initialized:
          self.activityIndicator.startAnimating(in: self.view)
        case .loading:
          guard viewModel.unifiedEvent != .loadingMoreFeeds else { return }
          self.activityIndicator.startAnimating(in: self.view)
        case .success(let feeds):
          DispatchQueue.main.async {
            self.updateData(withFeeds: feeds)
            self.activityIndicator.stopAnimating()
            self.hasAppliedInitialSnapshot = true
            self.handleAnchorIfReady()
          }
        case .failure(let errorMessage):
          self.loadMoreRequestMade = false
          self.activityIndicator.stopAnimating()
          print("Error: \(errorMessage)")
        }
      }
      .store(in: &initalFeedscancellables)
  }

  // swiftlint: disable cyclomatic_complexity function_body_length
  func handleEvents() {
    viewModel.unifiedEventPublisher
      .receive(on: RunLoop.main)
      .sink { [weak self] event in
        guard let self = self, let event = event else { return }
        switch event {
        case .castVoteSuccess(let response):
          self.handleCastVote(response)
        case .castVoteFailure(let error, let videoID):
          switch error {
          case .cloudFunctionError(let error):
            self.handleCastVoteFailure(error.description, videoID: videoID)
          default:
            print("Cast vote failure: \(error.localizedDescription)")
          }
        case .fetchingInitialFeeds:
          loadMoreRequestMade = true
        case .loadedMoreFeeds:
          DispatchQueue.main.async {
            self.shouldShowFooterLoader = false
            let snapshot = self.feedsDataSource.snapshot()
            self.applySnapshot(snapshot, animatingDifferences: true)
          }
        case .loadMoreFeedsFailed(let errorMessage):
          print("Load more feeds failed: \(errorMessage)")
        case .loadingMoreFeeds:
          self.loadMoreRequestMade = true
        case .finishedLoadingInitialFeeds:
          self.loadMoreRequestMade = false
        case .toggleLikeFailed(let errorMessage):
          print("Toggle like failed: \(errorMessage)")
        case .deleteVideoInitiated:
          self.activityIndicator.startAnimating(in: self.view)
        case .deleteVideoSuccess(let feeds):
          self.activityIndicator.stopAnimating()
          self.removeFeeds(with: feeds)
        case .deleteVideoFailed(let errorMessage):
          self.activityIndicator.stopAnimating()
          print("Delete video failed: \(errorMessage)")
        case .pageEndReached:
          pageEndReached = true
        case .reportInitiated:
          self.activityIndicator.startAnimating(in: self.view)
        case .reportSuccess(let postID):
          self.activityIndicator.stopAnimating()
          guard let feedIndex = self.feedsDataSource.snapshot().itemIdentifiers.firstIndex(
            where: {
              $0.postID == postID
            }
          ) else { return }
          self.removeFeeds(with: [self.feedsDataSource.snapshot().itemIdentifiers[feedIndex]], isReport: true)
        case .reportFailed(let error):
          self.activityIndicator.stopAnimating()
          print("Report video failed: \(error.localizedDescription)")
        case .blockedUser(let principalId):
          let itemsToRemove = self.feedsDataSource.snapshot().itemIdentifiers.filter { $0.principalID == principalId }
          self.removeFeeds(with: itemsToRemove, isReport: false)
        case .socialSignInSuccess, .socialSignInFailure:
          guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
          var snapshot = feedsDataSource.snapshot()
          let item = snapshot.itemIdentifiers[visibleIndexPath.item]
          snapshot.reloadItems([item])
          applySnapshot(snapshot, animatingDifferences: true)
          self.activityIndicator.stopAnimating()
        case .smileysFetched:
          self.activityIndicator.stopAnimating()
          var snapshot = feedsDataSource.snapshot()
          snapshot.reloadItems(snapshot.itemIdentifiers)
          applySnapshot(snapshot)
        case .feedsRefreshed:
          self.activityIndicator.startAnimating(in: self.view)
          applySnapshot(Snapshot())
        case .walletRechargeSuccess(let coins):
          if let indexPath = feedsCV.indexPathsForVisibleItems.first,
             let cell = feedsCV.cellForItem(at: indexPath) as? FeedsCell {
            cell.applyRechargeResult(.success, coins: coins)
          }
        case .walletRechargeFailure:
          if let indexPath = feedsCV.indexPathsForVisibleItems.first,
             let cell = feedsCV.cellForItem(at: indexPath) as? FeedsCell {
            cell.applyRechargeResult(.failure, coins: .zero)
          }
        case .fetchedDeeplinkFeed(let feed):
          insertOrMoveToFront(feed)
        }
      }
      .store(in: &paginatedFeedscancellables)
  }
  // swiftlint: enable function_body_length cyclomatic_complexity

  func setupNavigationBar() {
    navigationController?.navigationBar.isHidden = feedType == .otherUsers
    guard feedType == .currentUser else { return }
    let backImage = UIImage(named: Constants.navbackImage)
    let leftButton = UIBarButtonItem(image: backImage, style: .plain, target: self, action: #selector(didTapBack))
    leftButton.tintColor = Constants.navTitleTextColor
    navigationItem.leftBarButtonItem = leftButton

    let titleLabel = UILabel()
    titleLabel.text = Constants.navbarTitle
    titleLabel.textColor = Constants.navTitleTextColor
    titleLabel.font = Constants.navTitlefont
    navigationItem.titleView = titleLabel

    let appearance = UINavigationBarAppearance()
    appearance.configureWithTransparentBackground()
    appearance.backgroundColor = .clear
    appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
    navigationController?.navigationBar.standardAppearance = appearance
    navigationController?.navigationBar.scrollEdgeAppearance = appearance
  }

  @objc func didTapBack() {
    onBackButtonTap?()
  }

  func setupUI() {
    self.view.backgroundColor = .black
    setupCollectionView()
    setupActivityIndicator()
  }

  func setupCollectionView() {
    view.addSubview(feedsCV)
    let topAnchor: NSLayoutYAxisAnchor = (feedType == .currentUser) ?
    view.topAnchor : view.safeAreaLayoutGuide.topAnchor
    let bottomAnchor: NSLayoutYAxisAnchor = (feedType == .currentUser) ?
    view.bottomAnchor : view.safeAreaLayoutGuide.bottomAnchor
    NSLayoutConstraint.activate([
      feedsCV.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      feedsCV.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      feedsCV.topAnchor.constraint(equalTo: topAnchor),
      feedsCV.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -18)
    ])
    feedsCV.register(FeedsCell.self)
    feedsCV.register(
      FooterLoaderView.self,
      forSupplementaryViewOfKind: UICollectionView.elementKindSectionFooter
    )
    feedsCV.dataSource = feedsDataSource
    feedsCV.delegate = self
    feedsCV.setCollectionViewLayout(createLayout(), animated: false)
    configureFooter()
  }

  func setupActivityIndicator() {
    view.addSubview(activityIndicator)
    NSLayoutConstraint.activate([
      activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
      activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor),
      activityIndicator.widthAnchor.constraint(equalToConstant: Constants.indicatorSize),
      activityIndicator.heightAnchor.constraint(equalToConstant: Constants.indicatorSize)
    ])
  }

  func configureFooter() {
    feedsDataSource.supplementaryViewProvider = { [weak self] collectionView, kind, indexPath in
      guard let self = self,
            kind == UICollectionView.elementKindSectionFooter else {
        return UICollectionReusableView()
      }
      let footer = collectionView.dequeueReusableSupplementaryView(FooterLoaderView.self, ofKind: kind, for: indexPath)
      if self.loadMoreRequestMade && self.shouldShowFooterLoader && !pageEndReached {
        footer.startAnimating()
      } else {
        footer.stopAnimating()
      }
      return footer
    }
  }

  private func createLayout() -> UICollectionViewCompositionalLayout {
    return UICollectionViewCompositionalLayout { [weak self] (_, _) -> NSCollectionLayoutSection? in
      guard let self = self else { return nil }
      let itemSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(.one),
                                            heightDimension: .fractionalHeight(.one))
      let item = NSCollectionLayoutItem(layoutSize: itemSize)
      let group = NSCollectionLayoutGroup.vertical(layoutSize: itemSize, subitems: [item])
      let section = NSCollectionLayoutSection(group: group)
      let footerSize = NSCollectionLayoutSize(
        widthDimension: .fractionalWidth(.one),
        heightDimension: .estimated(Constants.footerHeight)
      )
      let footer = NSCollectionLayoutBoundarySupplementaryItem(
        layoutSize: footerSize,
        elementKind: UICollectionView.elementKindSectionFooter,
        alignment: .bottom
      )
      section.boundarySupplementaryItems = [footer]
      return section
    }
  }

  private func startLoadingBindings() {
    let feedsFinished = viewModel.unifiedStatePublisher
      .compactMap { [weak self] state -> Void? in
        guard let self = self else { return nil }
        if case .success = state { return () }
        if case .failure = state { return () }
        return nil
      }
      .prefix(1)

    let coinsReady = session.coinsReadyPublisher
      .prefix(1)

    Publishers.Zip(feedsFinished, coinsReady)
      .sink { [weak self] _, _ in
        guard let self else { return }
        Task {
          if self.feedType == .otherUsers {
            await self.viewModel.fetchSmileys()
          }
        }
      }
      .store(in: &coinFetchingCancellables)
  }

  func addAuthChangeListner() {
    let loginPhase = session.$state
      .map { [weak self] state -> Bool? in
        guard let self = self else { return nil }
        switch state {
        case .ephemeralAuthentication, .permanentAuthentication: return true
        case .loggedOut, .accountDeleted: return false
        default: return nil
        }
      }
      .removeDuplicates()
      .compactMap { $0 }
      .dropFirst()

    loginPhase
      .sink { [weak self] _ in
        guard let self else { return }
        Task { @MainActor in
          await self.viewModel.refreshFeeds()
        }
      }
      .store(in: &authStateCancellables)

    session.coinsReadyPublisher
      .dropFirst()
      .sink { [weak self] in
        guard let self else { return }
        Task { @MainActor in
          await self.viewModel.fetchFeeds(
            request: InitialFeedRequest(
              numResults: FeedsViewController.Constants.initialNumResults
            )
          )
          if self.feedType == .otherUsers {
            await self.viewModel.fetchSmileys()
          }
        }
      }
      .store(in: &authStateCancellables)
  }

  func addDeepLinkListner() {
    deepLinkRouter.$pendingDestination.sink { [weak self] destination in
      guard let self = self else { return }
      switch destination {
      case .openVideo(postId: let postId, canisterId: let canisterID):
        self.feedsCV.isHidden = true
        self.pendingAnchor = DeepLinkFeedRequest(
          postID: postId,
          canisterID: canisterID ?? "",
          numViewsAll: nil,
          numViewsLoggedIn: nil
        )
        self.handleAnchorIfReady()
      default: break
      }
    }
    .store(in: &deepLinkCancellables)
  }

  private func handleAnchorIfReady() {
    guard let anchor = pendingAnchor, hasAppliedInitialSnapshot else { return }
    pendingAnchor = nil

    Task { @MainActor in
      if let existingItem = findItem(postId: anchor.postID, canisterId: anchor.canisterID) {
        insertOrMoveToFront(existingItem)
        return
      }
      Task {
        await viewModel.fetchDeepLinkFeed(
          request: DeepLinkFeedRequest(
            postID: anchor.postID,
            canisterID: anchor.canisterID,
            numViewsAll: nil,
            numViewsLoggedIn: nil
          )
        )
      }
    }
  }

  @objc func appDidBecomeActive() {
    if isCurrentlyVisible {
      guard !feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
      feedsPlayer.play()
    }
  }

  deinit {
    NotificationCenter.default.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)
  }
}

extension FeedsViewController {
  enum Constants {
    static let maxFeedBatchSize = 100
    static let initialNumResults = 10
    static let thresholdForLoadingMoreResults = 6
    static let radius = 5
    static let shareURLPrefix = "https://yral.com/hot-or-not/"
    static let footerHeight = 50.0
    static let navBarHeight = 24.0
    static let navBackImageLeading = 16.0
    static let navbackImage = "chevron-left"
    static let navbarTitle = "Your Videos"
    static let navTitleTextColor = YralColor.grey0.uiColor
    static let navTitlefont = YralFont.pt20.bold.uiFont
    static let loaderLottie = "Yral_Loader"
    static let indicatorSize = 24.0

    static let deleteNudgeTitle = "Delete video?"
    static let deleteNudgeSubtitle = "This video will be permanently deleted from your Yral account."
    static let deleteConfirmButton = "Delete"
    static let deleteCancelButton = "Cancel"

    static let blockNudgeTitle = "Do you want to block this user?"
    // swiftlint: disable line_length
    static let blockNudgeSubtitle = "Blocking a user will hide their videos and activity from appearing in your home feed"
    // swiftlint: enable line_length
    static let blockConfirmButton = "Confrim"
    static let blockCancelButton = "Cancel"

    static let nsfwProbability = 0.4
    static let overlayIndex = 9

    static let winResult = "WIN"

    static let shareText = "Check out this video on Yral 👀 Where watching = fun + games! ⚡ Try it 👉"
    static let shareDescription = "Watch on Yral"
  }
}

protocol FeedsViewControllerRechargeDelegate: AnyObject {
  func walletAnimationStarted()
  func walletAnimationEnded(success: Bool, coins: Int64)
}
// swiftlint: enable type_body_length file_length
