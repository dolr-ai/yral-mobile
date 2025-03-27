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

  func removeFeeds(with feeds: [FeedResult], animated: Bool = false) {
    // 1. Get the current snapshot and delete the feed items from it.
    var snapshot = feedsDataSource.snapshot()
    snapshot.deleteItems(feeds)

    // 2. Remove the feeds from YralPlayer's internal feedResults.
    // Capture the IDs of the feeds to remove.
    let removedIDs = Set(feeds.map { $0.postID })

    // Save the currently playing feed's id (if any) before removal.
    let currentFeedID = yralPlayer.feedResults.indices.contains(yralPlayer.currentIndex)
      ? yralPlayer.feedResults[yralPlayer.currentIndex].postID
      : nil

    // Filter out the removed feeds from the player's feedResults.
    yralPlayer.feedResults = yralPlayer.feedResults.filter { !removedIDs.contains($0.postID) }

    // 3. Clear the cached AVPlayerItems since indices may have changed.
    // This forces the player to reload the necessary items.
    yralPlayer.playerItems.removeAll()

    // 4. If the currently playing feed was removed, adjust currentIndex and advance.
    if let currentFeedID = currentFeedID,
       !yralPlayer.feedResults.contains(where: { $0.postID == currentFeedID }) {
      // Choose the next valid index (or the last one if needed)
      let newIndex = min(yralPlayer.currentIndex, max(yralPlayer.feedResults.count - 1, 0))
      yralPlayer.currentIndex = newIndex
      yralPlayer.advanceToVideo(at: newIndex)
    }

    // 5. Apply the updated snapshot to update the UI.
    feedsDataSource.apply(snapshot, animatingDifferences: true)

    // 6. If no feeds remain, trigger the back action (e.g. to exit the view).
    if snapshot.itemIdentifiers.isEmpty {
      onBackButtonTap?()
    }
  }
}
