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

class FeedsViewController: UIViewController {
  typealias DataSource = UICollectionViewDiffableDataSource<Int, FeedResult>
  typealias Snapshot = NSDiffableDataSourceSnapshot<Int, FeedResult>

  var viewModel: FeedsViewModel
  var initalFeedscancellables: Set<AnyCancellable> = []
  var paginatedFeedscancellables: Set<AnyCancellable> = []
  lazy var yralPlayer: YralPlayer = {
    let player = YralPlayer()
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
    return collectionView
  }()

  lazy var activityIndicator: UIActivityIndicatorView = {
    let indicator = UIActivityIndicatorView(style: .medium)
    indicator.translatesAutoresizingMaskIntoConstraints = false
    indicator.color = .white
    indicator.hidesWhenStopped = true
    return indicator
  }()

  lazy var feedsDataSource = getConfiguredDataSource()
  var loadMoreRequestMade: Bool = false
  var shouldShowFooterLoader: Bool = false

  init(viewModel: FeedsViewModel) {
    self.viewModel = viewModel
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    bindViewModel()
    handleEvents()
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
    yralPlayer.pause()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    isCurrentlyVisible = true
    if feedsCV.indexPathsForVisibleItems.first != nil {
      yralPlayer.play()
    }
  }

  func bindViewModel() {
    viewModel.$state.receive(on: RunLoop.main).sink { [weak self] state in
      guard let self = self else { return }
      switch state {
      case .initalized:
        activityIndicator.startAnimating()
      case .loading:
        guard viewModel.event != .loadingMoreFeeds else { return }
        activityIndicator.startAnimating()
      case .successfullyFetched(let feeds):
        DispatchQueue.main.async {
          self.activityIndicator.stopAnimating()
          self.updateData(withFeeds: feeds)
        }
      case .failure(let error):
        activityIndicator.stopAnimating()
        loadMoreRequestMade = false
        print(error)
      }
    }.store(in: &initalFeedscancellables)
  }

  func handleEvents() {
    viewModel.$event.receive(on: RunLoop.main).sink { [weak self] event in
      guard let self = self else { return }
      switch event {
      case .loadedMoreFeeds:
        DispatchQueue.main.async {
          self.shouldShowFooterLoader = false
          let snapshot = self.feedsDataSource.snapshot()
          self.feedsDataSource.apply(snapshot, animatingDifferences: true)
        }
      case .loadMoreFeedsFailed(let error):
        print(error)
      case .fetchingInitialFeeds:
        loadMoreRequestMade = true
      case .finishedLoadingInitialFeeds:
        loadMoreRequestMade = false
      case .toggledLikeSuccessfully(let response):
        toggleLikeStatus(response)
      case .toggleLikeFailed(let error):
        print(error)
      default:
        break
      }
    }.store(in: &paginatedFeedscancellables)
  }

  func setupUI() {
    self.view.backgroundColor = .black
    setupCollectionView()
    setupActivityIndicator()
  }

  func setupCollectionView() {
    view.addSubview(feedsCV)
    NSLayoutConstraint.activate([
      feedsCV.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      feedsCV.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      feedsCV.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
      feedsCV.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor)
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
      activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
    ])
  }

  func configureFooter() {
    feedsDataSource.supplementaryViewProvider = { [weak self] collectionView, kind, indexPath in
      guard let self = self,
            kind == UICollectionView.elementKindSectionFooter else {
        return UICollectionReusableView()
      }
      let footer = collectionView.dequeueReusableSupplementaryView(FooterLoaderView.self, ofKind: kind, for: indexPath)
      if self.loadMoreRequestMade && self.shouldShowFooterLoader {
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
      yralPlayer.play()
    }
  }

  func storeThumbnail() {
    let currentTimeSec = yralPlayer.player.currentTime().seconds
    let roundedTime = String(format: "%.2f", currentTimeSec)
    guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
    let baseString = FeedsRepository.Constants.cloudfarePrefix +
    (feedsDataSource.itemIdentifier(for: visibleIndexPath)?.videoID ?? "") +
    FeedsRepository.Constants.thumbnailSuffix + "?time=\(roundedTime)s"
    guard let url = URL(string: baseString) else { return }
    lastDisplayedThumbnailPath[yralPlayer.currentIndex] = baseString
    SDWebImageManager.shared.loadImage(
      with: url,
      options: .highPriority,
      progress: nil
    ) { [weak self] image, _, error, _, _, _ in
      guard
        let image = image,
        error == nil
      else {
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
  }
}
