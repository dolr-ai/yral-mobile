//
//  FeedsViewController+DataSource.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import SDWebImage
import AVFoundation

extension FeedsViewController {
  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, feed in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      cell.delegate = self
      let lastDisplayedThumbnailPath = self.lastDisplayedThumbnailPath[indexPath.item] ?? ""

      if indexPath.row == self.yralPlayer.currentIndex {
        cell.configure(
          withPlayer: self.yralPlayer.player,
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked,
            lastThumbnailImage: SDImageCache.shared.imageFromCache(forKey: lastDisplayedThumbnailPath),
            feedType: self.feedType
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.principalID,
            subtitle: feed.postDescription
          ),
          index: indexPath.item
        )
      } else {
        cell.configure(
          withPlayer: AVPlayer(),
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked,
            lastThumbnailImage: SDImageCache.shared.imageFromCache(forKey: lastDisplayedThumbnailPath),
            feedType: self.feedType
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.principalID,
            subtitle: feed.postDescription
          ),
          index: indexPath.item
        )
      }
      return cell
    }
    return dataSource
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

    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate) { [weak self] in
      guard let self = self else { return }
      guard feedType == .currentUser else { return }
      yralPlayer.advanceToVideo(at: viewModel.getCurrentFeedIndex())
      self.feedsCV.scrollToItem(
        at: IndexPath(
          item: viewModel.getCurrentFeedIndex(),
          section: .zero
        ),
        at: .centeredVertically, animated: false
      )
    }
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
        if case .fetchingInitialFeeds = self.viewModel.unifiedEvent {
          return true
        }
        return false
      }()
    }
  }

  func toggleLikeStatus(_ response: LikeResult) {
    var snapshot = feedsDataSource.snapshot()
    var items = snapshot.itemIdentifiers
    items[response.index].isLiked = response.status
    items[response.index].likeCount += response.status ? .one : -.one

    snapshot.deleteItems(snapshot.itemIdentifiers)
    snapshot.appendItems(items)
    feedsDataSource.apply(snapshot, animatingDifferences: false)
  }
}
