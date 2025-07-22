//
//  FeedsViewController+ScrollView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import iosSharedUmbrella

extension FeedsViewController: UICollectionViewDelegate {
  func scrollViewDidScroll(_ scrollView: UIScrollView) {
    let offsetY = scrollView.contentOffset.y
    defer { lastContentOffsetY = offsetY }
    guard self.feedsDataSource.snapshot().numberOfItems > .zero else { return }
    let itemCount = feedsCV.numberOfItems(inSection: .zero)
    guard itemCount > .zero else { return }

    let goingDown = offsetY > lastContentOffsetY
    let current = feedsPlayer.currentIndex

    let candidate = goingDown ? current + .one : current - .one
    guard candidate >= .zero, candidate < itemCount else { return }

    guard let attrs = feedsCV.layoutAttributesForItem(at: IndexPath(item: candidate, section: .zero)) else { return }
    let visibleRect = CGRect(origin: scrollView.contentOffset, size: scrollView.bounds.size)
    let intersection = visibleRect.intersection(attrs.frame)
    guard !intersection.isNull else { return }
    let ratio = intersection.height / attrs.frame.height

    if ratio >= .half, candidate != current {
      let previous = current
      storeThumbnail()
      feedsPlayer.advanceToVideo(at: candidate)
      var snapshot = feedsDataSource.snapshot()
      let ids = snapshot.itemIdentifiers
      guard previous < ids.count, candidate < ids.count else { return }
      snapshot.reloadItems([ids[previous], ids[candidate]])
      feedsDataSource.apply(snapshot, animatingDifferences: false)
    } else {
      feedsPlayer.play()
    }
  }

  func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
    trackedVideoIDs.removeAll()
    storeThumbnail()
    feedsPlayer.pause()
  }

  func scrollViewWillEndDragging(
    _ scrollView: UIScrollView,
    withVelocity velocity: CGPoint,
    targetContentOffset: UnsafeMutablePointer<CGPoint>
  ) {
    feedsPlayer.play()
  }

  func scrollViewShouldScrollToTop(_ scrollView: UIScrollView) -> Bool {
    return false
  }

  func collectionView(
    _ collectionView: UICollectionView,
    willDisplay cell: UICollectionViewCell,
    forItemAt indexPath: IndexPath
  ) {
    guard let feedsCell = cell as? FeedsCell else { return }
    let item = feedsDataSource.snapshot().itemIdentifiers[indexPath.item]
    feedsCell.startListeningForFirstFrame()

    let feedsCount = feedsDataSource.snapshot().numberOfItems
    if indexPath.item >= feedsCount - Constants.thresholdForLoadingMoreResults, !loadMoreRequestMade {
      Task {
        self.loadMoreRequestMade = true
        await viewModel.loadMoreFeeds()
      }
    }
    if indexPath.item >= feedsCount - 1 {
      DispatchQueue.main.async {
        self.shouldShowFooterLoader = true
        let snapshot = self.feedsDataSource.snapshot()
        self.feedsDataSource.apply(snapshot, animatingDifferences: false)
      }
    }
    guard !trackedVideoIDs.contains(item.videoID) else { return }
    trackedVideoIDs.insert(item.videoID)
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoImpressionEventData(
        categoryName: self.feedType == .otherUsers ? .home : .profile,
        videoId: item.videoID,
        publisherUserId: item.principalID,
        likeCount: Int64(item.likeCount),
        shareCount: .zero,
        viewCount: item.viewCount,
        isGameEnabled: self.feedType == .otherUsers ? true : false,
        gameType: .smiley,
        isNsfw: false
      )
    )
  }

  func collectionView(
    _ collectionView: UICollectionView,
    didEndDisplaying cell: UICollectionViewCell,
    forItemAt indexPath: IndexPath
  ) {
    (cell as? FeedsCell)?.stopListeningForFirstFrame()
  }
}
