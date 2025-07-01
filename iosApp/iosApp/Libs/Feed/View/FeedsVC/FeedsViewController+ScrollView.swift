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
  func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
    guard scrollView.contentOffset.y > .zero else {
      storeThumbnail()
      return
    }
    feedsPlayer.pause()
    storeThumbnail()
  }

  func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
    feedsCV.layoutIfNeeded()
    guard let visibleIndexPath = feedsCV.indexPathsForVisibleItems.sorted().first else { return }
    let oldIndex = feedsPlayer.currentIndex
    let newIndex = visibleIndexPath.item
    if newIndex != oldIndex {
      if newIndex < feedsDataSource.snapshot().numberOfItems {
        let item = feedsDataSource.snapshot().itemIdentifiers[newIndex]
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
      feedsPlayer.advanceToVideo(at: newIndex)
      feedsCV.reloadData()
    } else {
      feedsPlayer.play()
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
    if indexPath.item >= feedsCount - 1 {
      DispatchQueue.main.async {
        self.shouldShowFooterLoader = true
        let snapshot = self.feedsDataSource.snapshot()
        self.feedsDataSource.apply(snapshot, animatingDifferences: false)
      }
    }
  }
}
