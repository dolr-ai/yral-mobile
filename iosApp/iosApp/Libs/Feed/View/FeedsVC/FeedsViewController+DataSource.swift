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
import iosSharedUmbrella

extension FeedsViewController {
  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, feed in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      cell.delegate = self
      // swiftlint: disable force_cast
        let showOnboarding = !(UserDefaultsManager.shared.get(for: DefaultsKey.onboardingCompleted) ?? false)
        && indexPath.item >= .three && indexPath.item % .three == .zero && !playToScroll && self.feedType == .otherUsers
        self.isTutorialVote = showOnboarding
        if showOnboarding {
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: GameTutorialShownEventData(
              videoId: feed.videoID,
              publisherUserId: feed.principalID,
              likeCount: Int64(feed.likeCount),
              shareCount: .zero,
              viewCount: feed.viewCount,
              gameType: .smiley,
              isNsfw: false
            )
          )
        }
        cell.configure(
          withPlayer: feedsPlayer as! FeedsPlayer,
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked,
            viewCount: feed.viewCount,
            feedType: self.feedType,
            showLoginOverlay: (
              indexPath.item != .zero &&
              indexPath.item % Constants.overlayIndex == .zero
            ) && !session.state.isLoggedIn,
            showOnboarding: showOnboarding
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.principalID,
            subtitle: feed.postDescription,
            coins: session.state.coins,
            position: session.state.dailyRank
          ),
          smileyGame: feed.smileyGame,
          session: session,
          index: indexPath.item
        )

      // swiftlint: enable force_cast
      return cell
    }
    return dataSource
  }

  func updateData(withFeeds feeds: [FeedResult], animated: Bool = false) {
    guard self.feedsDataSource.snapshot().itemIdentifiers.isEmpty else {
      self.addFeeds(with: feeds, animated: animated)
      return
    }
    guard !feeds.isEmpty else { return }

    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    self.feedsPlayer.loadInitialVideos(feeds)
    var snapshot = feedsDataSource.snapshot()
    if snapshot.sectionIdentifiers.isEmpty {
      snapshot.appendSections([.zero])
    }
    snapshot.appendItems(feeds, toSection: .zero)
    applySnapshot(snapshot, animatingDifferences: shouldAnimate) { [weak self] in
      guard let self = self else { return }
      guard feedType == .currentUser else { return }
      feedsPlayer.advanceToVideo(at: viewModel.getCurrentFeedIndex(), isDeepLink: false)
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
    feedsPlayer.addFeedResults(feeds)
    var shouldAnimate = false
    if #available(iOS 15, *) {
      shouldAnimate = animated
    }
    guard !self.feedsDataSource.snapshot().itemIdentifiers.isEmpty else { return }
    var snapshot = feedsDataSource.snapshot()
    snapshot.appendItems(feeds, toSection: .zero)
    applySnapshot(snapshot, animatingDifferences: shouldAnimate) {
      self.loadMoreRequestMade = {
        if case .fetchingInitialFeeds = self.viewModel.unifiedEvent {
          return true
        }
        return false
      }()
    }
  }

  func removeFeeds(with feeds: [FeedResult], isReport: Bool = false, animated: Bool = false) {
    var snapshot = feedsDataSource.snapshot()
    snapshot.deleteItems(feeds)
    applySnapshot(snapshot, animatingDifferences: animated) { [weak self] in
      guard let self else { return }
      feedsPlayer.removeFeeds(feeds)
      if snapshot.itemIdentifiers.isEmpty {
        feedsPlayer.pause()
      }
    }
    if isReport {
      DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
        ToastManager.showToast(type: .reportSuccess)
      }
    }
    if snapshot.itemIdentifiers.isEmpty {
      onBackButtonTap?()
    }
  }

  func findItem(postId: String, canisterId: String) -> FeedResult? {
    feedsDataSource.snapshot().itemIdentifiers.first {
      $0.postID == postId && $0.canisterID == canisterId
    }
  }

  func insertOrMoveToFront(_ feed: FeedResult) {
    var snapshot = feedsDataSource.snapshot()
    if snapshot.numberOfSections == .zero { snapshot.appendSections([.zero]) }

    if let duplicate = snapshot.itemIdentifiers.first(
      where: { $0.postID == feed.postID && $0.principalID == feed.principalID }
    ) {
      snapshot.deleteItems([duplicate])
      feedsPlayer.removeFeeds([duplicate])
    }

    if let firstItem = snapshot.itemIdentifiers.first {
      snapshot.insertItems([feed], beforeItem: firstItem)
    } else {
      snapshot.appendItems([feed], toSection: .zero)
    }
    feedsPlayer.insertFeed([feed], at: .zero)
    applySnapshot(snapshot, animatingDifferences: true) { [weak self] in
      guard let self = self else { return }
      self.feedsPlayer.advanceToVideo(at: .zero, isDeepLink: true)
      self.feedsCV.scrollToItem(
        at: IndexPath(
          item: self.viewModel.getCurrentFeedIndex(),
          section: .zero
        ),
        at: .centeredVertically, animated: false
      )
      self.feedsCV.isHidden = false
    }
  }

  func handleCastVote(_ response: SmileyGameResultResponse) {
    let snapshot = feedsDataSource.snapshot()
    let items = snapshot.itemIdentifiers
    guard let index = items.firstIndex(where: { $0.videoID == response.videoID }) else {
      return
    }

    guard let cell = feedsCV.cellForItem(at: IndexPath(item: index, section: 0)) as? FeedsCell else {
      updateUIAfterCastVoteSuccess(with: response)
      return
    }
    cell.startSmileyGamResultAnimation(for: response) { [weak self] in
      self?.updateUIAfterCastVoteSuccess(with: response)
    }
    guard index < snapshot.numberOfItems else {
      return
    }
    let item = items[index]
    let result = response.outcome == Constants.winResult ? GameResult.win : GameResult.loss

    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: GamePlayedEventData(
        videoId: item.videoID,
        publisherUserId: item.principalID,
        likeCount: Int64(item.likeCount),
        shareCount: Int64(.zero),
        viewCount: Int64(item.viewCount),
        gameType: .smiley,
        isNsfw: false,
        stakeAmount: Int32(item.smileyGame?.config.lossPenalty ?? .zero),
        stakeType: .yral,
        optionChosen: response.smiley.id,
        gameResult: result,
        wonLossAmount: Int32(abs(response.coinDelta)),
        isTutorialVote: isTutorialVote
      )
    )
  }

  private func updateUIAfterCastVoteSuccess(with response: SmileyGameResultResponse) {
    var snap = feedsDataSource.snapshot()
    guard let oldItem = snap.itemIdentifiers.first(where: { $0.videoID == response.videoID }) else {
      return
    }

    var newItem = oldItem
    newItem.smileyGame?.state = .played(response)

    if let pos = snap.itemIdentifiers.firstIndex(of: oldItem) {
      snap.deleteItems([oldItem])
      if pos < snap.itemIdentifiers.count {
        let next = snap.itemIdentifiers[pos]
        snap.insertItems([newItem], beforeItem: next)
      } else {
        snap.appendItems([newItem])
      }
    }

    feedsDataSource.apply(snap, animatingDifferences: true)
    let oldBalance = session.state.coins
    let updatedBalance = Int(oldBalance) + response.coinDelta
    session.update(coins: UInt64(updatedBalance))
    session.update(dailyRank: response.newPosition)
  }

  func updateUIAfterGamePlayed(for videoID: String) {
    var snap = feedsDataSource.snapshot()
    guard let oldItem = snap.itemIdentifiers.first(where: { $0.videoID == videoID }) else {
      return
    }

    var newItem = oldItem
    newItem.smileyGame?.state = .voted

    if let pos = snap.itemIdentifiers.firstIndex(of: oldItem) {
      snap.deleteItems([oldItem])
      if pos < snap.itemIdentifiers.count {
        let next = snap.itemIdentifiers[pos]
        snap.insertItems([newItem], beforeItem: next)
      } else {
        snap.appendItems([newItem])
      }
    }

    feedsDataSource.apply(snap, animatingDifferences: true)
  }

  func handleCastVoteFailure(_ errorMessage: String, videoID: String) {
    var snap = feedsDataSource.snapshot()
    guard let oldItem = snap.itemIdentifiers.first(where: { $0.videoID == videoID }) else {
      return
    }

    if let idx = snap.itemIdentifiers.firstIndex(of: oldItem),
       let cell = feedsCV.cellForItem(at: IndexPath(item: idx, section: 0)) as? FeedsCell {
      cell.handleSmileyGameError(errorMessage)
    }

    var newItem = oldItem
    newItem.smileyGame?.state = .error(errorMessage)

    if let pos = snap.itemIdentifiers.firstIndex(of: oldItem) {
      snap.deleteItems([oldItem])
      if pos < snap.itemIdentifiers.count {
        let next = snap.itemIdentifiers[pos]
        snap.insertItems([newItem], beforeItem: next)
      } else {
        snap.appendItems([newItem])
      }
    }
    applySnapshot(snap, animatingDifferences: true)
  }
}

extension FeedsViewController {
  func applySnapshot(
    _ snapshot: Snapshot,
    animatingDifferences: Bool = false,
    completion: (() -> Void)? = nil
  ) {
    isApplyingSnapshot = true
    feedsDataSource.apply(snapshot, animatingDifferences: animatingDifferences) { [weak self] in
      completion?()
      self?.isApplyingSnapshot = false
    }
  }
}
