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
  private var feedsCV: UICollectionView = {
    let collectionView = getUICollectionView()
    collectionView.showsVerticalScrollIndicator = false
    collectionView.showsHorizontalScrollIndicator = false
    collectionView.backgroundColor = .clear
    collectionView.isPagingEnabled = true
    return collectionView
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
      await viewModel.fetchFeeds(request: FeedRequest(filteredPosts: [], numResults: Constants.initialNumResults))
    }
  }

  func bindViewModel() {
    viewModel.$state.receive(on: RunLoop.main).sink { [weak self] state in
      guard let self = self else { return }
      switch state {
      case .initalized:
        break
      case .loading:
        break
      case .successfullyFetched(let feeds):
        self.updateData(withFeeds: feeds)
      case .failure(let error):
        print(error)
      }
    }.store(in: &initalFeedscancellables)
  }

  func handleEvents() {
    viewModel.$event.receive(on: RunLoop.main).sink { [weak self] event in
      guard let self = self else { return }
      switch event {
      case .loadedMoreFeeds(let feeds):
        self.addFeeds(with: feeds)
      case .loadMoreFeedsFailed(let error):
        print(error)
        self.loadMoreRequestMade = false
      default:
        break
      }
    }.store(in: &paginatedFeedscancellables)
  }
  func setupUI() {
    self.view.backgroundColor = .black
    setupCollectionView()
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
    view.layoutIfNeeded()
    feedsCV.setCollectionViewLayout(createLayout(), animated: false)
  }

  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, feed in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      if indexPath.row == self.yralPlayer.currentIndex {
        cell.configure(
          withPlayer: self.yralPlayer.player,
          thumbnailURL: feed.thumbnail,
          lastFrameImage: nil
        )
      } else {
        cell.configure(
          withPlayer: AVPlayer(),
          thumbnailURL: feed.thumbnail,
          lastFrameImage: nil
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
    guard self.feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    self.yralPlayer.loadInitialVideos(feeds)
    var snapshot = feedsDataSource.snapshot()
    snapshot.appendSections([0])
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
      self.loadMoreRequestMade = false
    }
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
    if indexPath.item == feedsCount - Constants.thresholdForLoadingMoreResults, !loadMoreRequestMade {
      Task {
        self.loadMoreRequestMade = true
        await viewModel.loadMoreFeeds()
      }
    }
  }
}

extension FeedsViewController {
  enum Constants {
    static let initialNumResults = 10
    static let thresholdForLoadingMoreResults = 6
    static let radius = 5
  }
}
