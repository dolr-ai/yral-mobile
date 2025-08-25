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

    guard feedsDataSource.snapshot().numberOfItems > 0 else { return }
    let itemCount = feedsCV.numberOfItems(inSection: 0)
    guard itemCount > 0 else { return }

    let visibleRect = CGRect(origin: scrollView.contentOffset, size: scrollView.bounds.size)
    guard
      let attrsInRect = feedsCV.collectionViewLayout
        .layoutAttributesForElements(in: visibleRect),
      !attrsInRect.isEmpty
    else { return }

    let bestPair: (IndexPath, CGFloat)? = attrsInRect
      .map { attributes -> (IndexPath, CGFloat) in
        let intersection = visibleRect.intersection(attributes.frame)
        let ratio = intersection.isNull ? 0 : (intersection.height / attributes.frame.height)
        return (attributes.indexPath, ratio)
      }
      .max(by: { $0.1 < $1.1 })

    guard let (bestIP, bestRatio) = bestPair else { return }
    let candidate = bestIP.item
    guard candidate >= 0 && candidate < itemCount else { return }

    let playing = feedsPlayer.currentIndex
    guard candidate != playing, bestRatio >= .half else {
      feedsPlayer.play()
      return
    }

    var snapshot = feedsDataSource.snapshot()
    let ids = snapshot.itemIdentifiers
    guard playing < ids.count, candidate < ids.count else { return }
    self.feedsPlayer.incrementIndex()
    snapshot.reloadItems([ids[playing], ids[candidate]])
    feedsDataSource.apply(snapshot, animatingDifferences: false) { [weak self] in
      guard self != nil else { return }
      self?.feedsPlayer.decrementIndex()
      self?.feedsPlayer.advanceToVideo(at: candidate)
    }
  }

  func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
    trackedVideoIDs.removeAll()
    feedsPlayer.pause()

    guard playToScroll else { return }

    let velocityY = scrollView.panGestureRecognizer.velocity(in: scrollView).y
    let feedItemIndex = feedsCV.indexPathsForVisibleItems.first?.item ?? 0
    guard feedItemIndex < feedsDataSource.snapshot().itemIdentifiers.count else { return }
    let smileyGameState = feedsDataSource.snapshot().itemIdentifiers[feedItemIndex].smileyGame?.state
    if velocityY < .zero, !isShowingPlayToScroll, case .notPlayed = smileyGameState {
      isShowingPlayToScroll = true

      let lockOffset = CGPoint(x: scrollView.contentOffset.x, y: lastContentOffsetY)
      scrollView.setContentOffset(lockOffset, animated: false)

      scrollView.panGestureRecognizer.isEnabled = false
      scrollView.panGestureRecognizer.isEnabled = true

      if let cell = feedsCV.visibleCells.first as? FeedsCell {
        cell.showPlayToScroll()
      }
    }
  }

  func scrollViewWillEndDragging(
    _ scrollView: UIScrollView,
    withVelocity velocity: CGPoint,
    targetContentOffset: UnsafeMutablePointer<CGPoint>
  ) {
    feedsPlayer.play()
  }

  func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
    Task.detached {
      await self.feedsPlayer.cancelPreloadOutsideRange(center: self.feedsPlayer.currentIndex)
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
    isShowingPlayToScroll = false
  }
}
