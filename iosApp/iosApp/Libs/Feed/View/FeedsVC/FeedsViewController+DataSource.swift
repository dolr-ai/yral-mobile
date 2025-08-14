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
  // swiftlint: disable function_body_length
  func getConfiguredDataSource() -> DataSource {
    let dataSource = DataSource(collectionView: feedsCV) { [weak self] collectionView, indexPath, feed in
      guard let self = self else { return UICollectionViewCell() }
      let cell = collectionView.dequeueReusableCell(FeedsCell.self, for: indexPath)
      cell.delegate = self
      let lastDisplayedThumbnailPath = self.lastDisplayedThumbnailPath[feed.videoID] ?? ""
      // swiftlint: disable force_cast
      if indexPath.row == self.feedsPlayer.currentIndex {
        cell.configure(
          withPlayer: feedsPlayer as! FeedsPlayer,
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked,
            lastThumbnailImage: SDImageCache.shared.imageFromCache(forKey: lastDisplayedThumbnailPath),
            feedType: self.feedType,
            showLoginOverlay: (
              indexPath.item != .zero &&
              indexPath.item % Constants.overlayIndex == .zero
            ) && !session.state.isLoggedIn,
            showOnboarding: !(UserDefaultsManager.shared.get(for: DefaultsKey.onboardingCompleted) ?? false)
            && indexPath.item >= .three
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.principalID,
            subtitle: feed.postDescription,
            coins: session.state.coins
          ),
          smileyGame: feed.smileyGame,
          session: session,
          index: indexPath.item
        )
      } else {
        cell.configure(
          withPlayer: nil,
          feedInfo: FeedsCell.FeedCellInfo(
            thumbnailURL: feed.thumbnail,
            likeCount: feed.likeCount,
            isLiked: feed.isLiked,
            lastThumbnailImage: SDImageCache.shared.imageFromCache(forKey: lastDisplayedThumbnailPath),
            feedType: self.feedType,
            showLoginOverlay: (
              indexPath.item != .zero &&
              indexPath.item % Constants.overlayIndex == .zero
            ) && !session.state.isLoggedIn,
            showOnboarding: false
          ),
          profileInfo: ProfileInfoView.ProfileInfo(
            imageURL: feed.profileImageURL,
            title: feed.principalID,
            subtitle: feed.postDescription,
            coins: session.state.coins
          ),
          smileyGame: feed.smileyGame,
          session: session,
          index: indexPath.item
        )
      }
      // swiftlint: enable force_cast
      return cell
    }
    return dataSource
  }
  // swiftlint: enable function_body_length

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

    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate) { [weak self] in
      guard let self = self else { return }
      guard feedType == .currentUser else { return }
      feedsPlayer.advanceToVideo(at: viewModel.getCurrentFeedIndex())
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
    feedsDataSource.apply(snapshot, animatingDifferences: shouldAnimate) {
      self.loadMoreRequestMade = {
        if case .fetchingInitialFeeds = self.viewModel.unifiedEvent {
          return true
        }
        return false
      }()
    }
  }

  func handleCastVote(_ response: SmileyGameResultResponse) {
    let snapshot = feedsDataSource.snapshot()
    let items = snapshot.itemIdentifiers
    guard let index = items.firstIndex(where: { $0.videoID == response.videoID }) else {
      return
    }

    guard let cell = feedsCV.cellForItem(at: IndexPath(item: index, section: 0)) as? FeedsCell else {
      updateUIAfterCastVoteSuccess(
        with: response,
        forIndex: index,
        withItems: items,
        andSnapshot: snapshot
      )
      return
    }
    cell.startSmileyGamResultAnimation(for: response) { [weak self] in
      self?.updateUIAfterCastVoteSuccess(
        with: response,
        forIndex: index,
        withItems: items,
        andSnapshot: snapshot
      )
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
        isTutorialVote: false
      )
    )
  }

  private func updateUIAfterCastVoteSuccess(
    with response: SmileyGameResultResponse,
    forIndex index: Int,
    withItems items: [FeedResult],
    andSnapshot snapshot: NSDiffableDataSourceSnapshot<Int, FeedResult>
  ) {
    var items = items
    items[index].smileyGame?.state = .played(response)

    var snapshot = snapshot
    snapshot.deleteItems(snapshot.itemIdentifiers)
    snapshot.appendItems(items)

    self.feedsDataSource.apply(snapshot, animatingDifferences: true)
    self.session.update(coins: response.coins)
  }

  func handleCastVoteFailure(_ errorMessage: String, videoID: String) {
    var snapshot = feedsDataSource.snapshot()
    var items = snapshot.itemIdentifiers
    guard let index = items.firstIndex(where: { $0.videoID == videoID }) else {
      return
    }

    guard let cell = feedsCV.cellForItem(at: IndexPath(item: index, section: 0)) as? FeedsCell else {
      return
    }

    cell.handleSmileyGameError(errorMessage)

    items[0].smileyGame?.state = .error(errorMessage)

    snapshot.deleteItems(snapshot.itemIdentifiers)
    snapshot.appendItems(items)
    feedsDataSource.apply(snapshot, animatingDifferences: true)
  }

  func removeFeeds(with feeds: [FeedResult], isReport: Bool = false, animated: Bool = false) {
    for feed in feeds {
      lastDisplayedThumbnailPath.removeValue(forKey: feed.videoID)
    }
    var snapshot = feedsDataSource.snapshot()
    snapshot.deleteItems(feeds)
    feedsDataSource.apply(snapshot, animatingDifferences: animated) { [weak self] in
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
}
