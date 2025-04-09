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

// swiftlint: disable type_body_length
class FeedsViewController: UIViewController {
  typealias DataSource = UICollectionViewDiffableDataSource<Int, FeedResult>
  typealias Snapshot = NSDiffableDataSourceSnapshot<Int, FeedResult>

  let viewModel: any FeedViewModelProtocol
  var feedType: FeedType = .otherUsers
  var initalFeedscancellables: Set<AnyCancellable> = []
  var paginatedFeedscancellables: Set<AnyCancellable> = []

    lazy var feedsPlayer: YralPlayer = {
        let player = FeedsPlayer()
        player.delegate = self
        return player
    }()
    var isCurrentlyVisible = true
    var lastDisplayedThumbnailPath: [Int: String] = [:]

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

  init(viewModel: any FeedViewModelProtocol, feedType: FeedType = .otherUsers) {
    self.viewModel = viewModel
    self.feedType = feedType
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
    Task { @MainActor in
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
    yralPlayer.isPlayerVisible = false
    yralPlayer.pause()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    isCurrentlyVisible = true
    yralPlayer.isPlayerVisible = true
    guard !feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
    yralPlayer.play()
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
            self.activityIndicator.stopAnimating()
            self.updateData(withFeeds: feeds)
          }
        case .failure(let errorMessage):
          self.activityIndicator.stopAnimating()
          self.loadMoreRequestMade = false
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
        case .fetchingInitialFeeds:
          loadMoreRequestMade = true
        case .loadedMoreFeeds:
          DispatchQueue.main.async {
            self.shouldShowFooterLoader = false
            let snapshot = self.feedsDataSource.snapshot()
            self.feedsDataSource.apply(snapshot, animatingDifferences: true)
          }
        case .loadMoreFeedsFailed(let errorMessage):
          print("Load more feeds failed: \(errorMessage)")
        case .loadingMoreFeeds:
          self.loadMoreRequestMade = true
        case .finishedLoadingInitialFeeds:
          self.loadMoreRequestMade = false
        case .toggledLikeSuccessfully(let response):
          self.toggleLikeStatus(response)
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
        }
      }
      .store(in: &paginatedFeedscancellables)
  }
  // swiftlint: enable cyclomatic_complexity function_body_length

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
      feedsCV.bottomAnchor.constraint(equalTo: bottomAnchor)
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
    return UICollectionViewCompositionalLayout { (_, _) -> NSCollectionLayoutSection? in
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

  @objc func appDidBecomeActive() {
    if isCurrentlyVisible {
      guard !feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
      yralPlayer.play()
    }
  }

  func storeThumbnail() {
    let currentTimeSec = yralPlayer.player.currentTime().seconds
    let roundedTime = String(format: "%.2f", currentTimeSec)
    guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
    let videoID = feedsDataSource.itemIdentifier(for: visibleIndexPath)?.videoID ?? ""
    let baseString = FeedsRepository.Constants.cloudfarePrefix + videoID +
    FeedsRepository.Constants.thumbnailSuffix + "?time=\(roundedTime)s"
    guard let url = URL(string: baseString) else { return }
    lastDisplayedThumbnailPath[videoID] = baseString
    SDWebImageManager.shared.loadImage(
      with: url,
      options: .highPriority,
      progress: nil
    ) { [weak self] image, _, error, _, _, _ in
      guard let image = image, error == nil else {
        return
      }
      SDImageCache.shared.store(image, forKey: baseString, completion: nil)
    }
  }

  deinit {
    NotificationCenter.default.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)
  }
}

extension FeedsViewController {
  enum Constants {
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
  }
}
// swiftlint: enable type_body_length
