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
  private var cancellables: Set<AnyCancellable> = []
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

  init(viewModel: FeedsViewModel) {
    self.viewModel = viewModel
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    bindViewModel(viewModel: viewModel)
    setupUI()
    Task { @MainActor in
      await viewModel.fetchFeeds(request: FeedRequest(filteredPosts: [], numResults: Constants.initialNumResults))
    }
  }

  func bindViewModel(viewModel: FeedsViewModel) {
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
    }.store(in: &cancellables)
  }

  func setupUI() {
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
    let layout = UICollectionViewFlowLayout()
    layout.scrollDirection = .vertical
    layout.itemSize = CGSize(width: view.bounds.width, height: view.bounds.height)
    layout.minimumLineSpacing = .zero
    feedsCV.setCollectionViewLayout(createLayout(), animated: false)
  }

  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, _ in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      if indexPath.row == self.yralPlayer.currentIndex {
        cell.configure(withPlayer: self.yralPlayer.player)
      } else {
        cell.configure(withPlayer: AVPlayer())
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
    self.yralPlayer.loadInitialVideos(feeds)
    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    var snapshot = feedsDataSource.snapshot()
    snapshot.appendSections([0])
    snapshot.appendItems(feeds, toSection: 0)
    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate)
  }
}

extension FeedsViewController: UICollectionViewDelegate {
  func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
    yralPlayer.player.pause()
  }

  func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
    feedsCV.layoutIfNeeded() // Force layout update
    guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
    let oldIndex = yralPlayer.currentIndex
    let newIndex = visibleIndexPath.item
    if newIndex != oldIndex {
      yralPlayer.advanceToVideo(at: newIndex)
      feedsCV.reloadData()
    }
  }

  func collectionView(
    _ collectionView: UICollectionView,
    willDisplay cell: UICollectionViewCell,
    forItemAt indexPath: IndexPath
  ) {
    let feedsCount = feedsDataSource.snapshot().numberOfItems
    let threshold = 1
    if indexPath.item == feedsCount - threshold {
      Task {
//        await viewModel.fetchFeeds(request: FeedRequest(filteredPosts: [], numResults: 5))
      }
    }
  }
}

extension FeedsViewController {
  enum Constants {
    static let initialNumResults = 5
    static let thresholdForLoadingMoreResults = 1
  }
}
