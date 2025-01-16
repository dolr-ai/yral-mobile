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

class FeedsViewController: UIViewController {
  typealias DataSource = UICollectionViewDiffableDataSource<Int, FeedResult>
  typealias Snapshot = NSDiffableDataSourceSnapshot<Int, FeedResult>

  private var viewModel: FeedsViewModel
  private var initalFeedscancellables: Set<AnyCancellable> = []
  private var paginatedFeedscancellables: Set<AnyCancellable> = []
  private var yralPlayer = YralPlayer()
  private var isCurrentlyVisible = true

  private var feedsCV: UICollectionView = {
    let collectionView = getUICollectionView()
    collectionView.showsVerticalScrollIndicator = false
    collectionView.showsHorizontalScrollIndicator = false
    collectionView.backgroundColor = .clear
    collectionView.isPagingEnabled = true
    collectionView.scrollsToTop = false
    return collectionView
  }()

  private lazy var activityIndicator: UIActivityIndicatorView = {
    let indicator = UIActivityIndicatorView(style: .medium)
    indicator.translatesAutoresizingMaskIntoConstraints = false
    indicator.color = .white
    indicator.hidesWhenStopped = true
    return indicator
  }()

  lazy var feedsDataSource = getConfiguredDataSource()
  private var loadMoreRequestMade: Bool = false

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
        break
      case .successfullyFetched(let feeds):
        activityIndicator.stopAnimating()
        self.updateData(withFeeds: feeds)
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
        break
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
    feedsCV.dataSource = feedsDataSource
    feedsCV.delegate = self
    feedsCV.setCollectionViewLayout(createLayout(), animated: false)
  }

  func setupActivityIndicator() {
    view.addSubview(activityIndicator)
    NSLayoutConstraint.activate([
      activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
      activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
    ])
  }

  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, feed in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      cell.delegate = self
      if indexPath.row == self.yralPlayer.currentIndex {
        cell.configure(
          withPlayer: self.yralPlayer.player,
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            lastFrameImage: nil,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.canisterID,
            subtitle: feed.postDescription),
          index: indexPath.item
        )
      } else {
        cell.configure(
          withPlayer: AVPlayer(),
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            lastFrameImage: nil,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.canisterID,
            subtitle: feed.postDescription),
          index: indexPath.item
        )
      }
      return cell
    }
    return dataSource
  }

  private func createLayout() -> UICollectionViewCompositionalLayout {
    return UICollectionViewCompositionalLayout { (_, _) -> NSCollectionLayoutSection? in
      let itemSize = NSCollectionLayoutSize(widthDimension: .fractionalWidth(.one),
                                            heightDimension: .fractionalHeight(.one))
      let item = NSCollectionLayoutItem(layoutSize: itemSize)
      let group = NSCollectionLayoutGroup.vertical(layoutSize: itemSize, subitems: [item])
      let section = NSCollectionLayoutSection(group: group)
      return section
    }
  }

  func updateData(withFeeds feeds: [FeedResult], animated: Bool = false) {
    guard self.feedsDataSource.snapshot().itemIdentifiers.isEmpty else {
      self.addFeeds(with: feeds, animated: animated)
      return
    }
    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    self.yralPlayer.loadInitialVideos(feeds)
    var snapshot = feedsDataSource.snapshot()
    snapshot.appendSections([.zero])
    snapshot.appendItems(feeds, toSection: .zero)
    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate)
  }

  func addFeeds(with feeds: [FeedResult], animated: Bool = false) {
    yralPlayer.addFeedResults(feeds)
    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    guard !self.feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
    var snapshot = feedsDataSource.snapshot()
    snapshot.appendItems(feeds, toSection: .zero)
    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate) {
      self.loadMoreRequestMade = {
        if case .fetchingInitialFeeds = self.viewModel.event {
          return true
        }
        return false
      }()
    }
  }

  private func toggleLikeStatus(_ response: LikeResult) {
    var snapshot = feedsDataSource.snapshot()
    var items = snapshot.itemIdentifiers
    items[response.index].isLiked = response.status
    items[response.index].likeCount += response.status ? .one : -.one

    snapshot.deleteItems(snapshot.itemIdentifiers)
    snapshot.appendItems(items)
    feedsDataSource.apply(snapshot, animatingDifferences: false)
  }

  @objc func appDidBecomeActive() {
    if isCurrentlyVisible {
      yralPlayer.play()
    }
  }

  deinit {
    NotificationCenter.default.removeObserver(self, name: UIApplication.didBecomeActiveNotification, object: nil)
  }
}

extension FeedsViewController: UICollectionViewDelegate {
  func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
    guard !loadMoreRequestMade, scrollView.contentOffset.y > .zero else { return }
    yralPlayer.pause()
  }

  func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
    feedsCV.layoutIfNeeded()
    guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
    let oldIndex = yralPlayer.currentIndex
    let newIndex = visibleIndexPath.item
    if newIndex != oldIndex {
      yralPlayer.advanceToVideo(at: newIndex)
      feedsCV.reloadData()
    } else {
      yralPlayer.play()
    }
  }

  func scrollViewShouldScrollToTop(_ scrollView: UIScrollView) -> Bool {
    return false
  }

  func collectionView(
    _ collectionView: UICollectionView,
    willDisplay cell: UICollectionViewCell,
    forItemAt indexPath: IndexPath
  ) {
    let feedsCount = feedsDataSource.snapshot().numberOfItems
    if indexPath.item >= feedsCount - Constants.thresholdForLoadingMoreResults, !loadMoreRequestMade {
      Task {
        self.loadMoreRequestMade = true
        await viewModel.loadMoreFeeds()
      }
    }
  }
}

extension FeedsViewController: FeedsCellProtocol {
  func shareButtonTapped(index: Int) {
    activityIndicator.startAnimating()
    // swiftlint: disable line_length
    guard let shareURL = URL(
      string: "https://yral.com/hot-or-not/\(self.feedsDataSource.snapshot().itemIdentifiers[index].canisterID)/\(self.feedsDataSource.snapshot().itemIdentifiers[index].postID)"
    ) else { return }
    // swiftlint: enable line_length
    let activityViewController = UIActivityViewController(
      activityItems: [shareURL],
      applicationActivities: nil
    )

    guard let viewController = navigationController?.viewControllers.first else { return }
    viewController.present(activityViewController, animated: true) {
      self.activityIndicator.stopAnimating()
    }
  }

  func likeButtonTapped(index: Int) {
    guard let postID = Int(feedsDataSource.snapshot().itemIdentifiers[index].postID) else { return }
    let canisterID = feedsDataSource.snapshot().itemIdentifiers[index].canisterID
    Task { @MainActor in
      await self.viewModel.toggleLike(request: LikeQuery(postID: postID, canisterID: canisterID, index: index))
    }
    guard let cell = feedsCV.cellForItem(at: IndexPath(item: index, section: .zero)) as? FeedsCell else { return }
    cell.setLikeStatus(isLiked: cell.likeButton.configuration?.image == FeedsCell.Constants.likeUnSelectedImage)
  }
}

extension FeedsViewController {
  enum Constants {
    static let initialNumResults = 10
    static let thresholdForLoadingMoreResults = 6
    static let radius = 5
    static let shareURLPrefix = "https://yral.com/hot-or-not/"
  }
}
