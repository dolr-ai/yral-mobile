//
//  FeedsViewController+Delegates.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import SwiftUI
import iosSharedUmbrella

extension FeedsViewController: FeedsCellProtocol {
  func makeSmileyGameRulesDIContainer() -> SmileyGameRuleDIContainer {
    return SmileyGameRuleDIContainer(
      dependencies: SmileyGameRuleDIContainer.Dependencies(
        firebaseService: FirebaseService(),
        crashReporter: CompositeCrashReporter(reporters: [FirebaseCrashlyticsReporter()])
      )
    )
  }

  fileprivate func smileyGameSheetEvent(
    _ feed: FeedResult,
    _ result: GameResult,
    _ sheetCtaType: GameConcludedCtaType,
    coinDelta: Int
  ) {
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: GameConcludedBottomsheetClickedEventData(
        stakeAmount: Int32(feed.smileyGame?.config.lossPenalty ?? .zero),
        stakeType: .sats,
        gameResult: result,
        wonLossAmount: Int32(abs(coinDelta)),
        ctaType: sheetCtaType
      )
    )
  }

  func showGameResultBottomSheet(index: Int, gameResult: SmileyGameResultResponse) {
    var hostingController: UIHostingController<SmileyGameResultBottomSheetView>?

    let bottomSheetView = SmileyGameResultBottomSheetView(
      gameResult: gameResult) {
        let feed = self.feedsDataSource.snapshot().itemIdentifiers[index]
        let sheetCtaType = GameConcludedCtaType.keepPlaying
        let result = gameResult.outcome == Constants.winResult ? GameResult.win : GameResult.loss
        self.smileyGameSheetEvent(feed, result, sheetCtaType, coinDelta: gameResult.coinDelta)
        hostingController?.dismiss(animated: true)
      } onLearnMoreTapped: {
        let feed = self.feedsDataSource.snapshot().itemIdentifiers[index]
        let sheetCtaType = GameConcludedCtaType.learnMore
        let result = gameResult.outcome == Constants.winResult ? GameResult.win : GameResult.loss
        self.smileyGameSheetEvent(feed, result, sheetCtaType, coinDelta: gameResult.coinDelta)
        hostingController?.dismiss(animated: true) {
          let smileyGameRuleView = self.makeSmileyGameRulesDIContainer().makeSmileyGameRuleView {
            self.navigationController?.popViewController(animated: true)
          }
          let smileyGameRuleVC = UIHostingController(rootView: smileyGameRuleView)
          smileyGameRuleVC.extendedLayoutIncludesOpaqueBars = true
          self.navigationController?.pushViewController(smileyGameRuleVC, animated: true)
        }
      }

    hostingController = UIHostingController(rootView: bottomSheetView)
    hostingController!.modalPresentationStyle = .overFullScreen
    hostingController!.modalTransitionStyle = .crossDissolve
    hostingController?.view.backgroundColor = .clear
    self.present(hostingController!, animated: true, completion: nil)
  }

  func shareButtonTapped(index: Int) {
    activityIndicator.startAnimating(in: self.view)
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

  func smileyTapped(index: Int, smiley: Smiley) {
    let item = feedsDataSource.snapshot().itemIdentifiers[index]
    let videoID = item.videoID
    let smileyID = smiley.id

    Task { @MainActor in
      await self.viewModel.castVote(request: CastVoteQuery(videoID: videoID, smileyID: smileyID))
    }
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: GameVotedEventData(
        videoId: videoID,
        publisherUserId: item.principalID,
        likeCount: Int64(item.likeCount),
        shareCount: Int64(.zero),
        viewCount: Int64(item.viewCount),
        gameType: .smiley,
        isNsfw: false,
        stakeAmount: Int32(item.smileyGame?.config.lossPenalty ?? Int.zero),
        stakeType: StakeType.sats,
        optionChosen: smileyID
      )
    )
  }

  func deleteButtonTapped(index: Int) {
    getNudgeView(at: index, isDelete: true)
    let snapshot = feedsDataSource.snapshot()
    guard index < snapshot.itemIdentifiers.count else { return }
    let item = snapshot.itemIdentifiers[index]
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoClickedEventData(
        videoId: item.videoID,
        publisherUserId: item.principalID,
        likeCount: Int64(item.likeCount),
        shareCount: .zero,
        viewCount: item.viewCount,
        isGameEnabled: true,
        gameType: .smiley,
        isNsfw: false,
        ctaType: .delete_,
        pageName: self.feedType == .otherUsers ? .home : .profile
      )
    )
  }

  func reportButtonTapped(index: Int) {
    let snapshot = feedsDataSource.snapshot()
    guard index < snapshot.itemIdentifiers.count else { return }
    let feedItem = snapshot.itemIdentifiers[index]

    var hostingController: UIHostingController<ReportNudgeView>?

    let reportView = ReportNudgeView(
      onSubmit: { [weak self] reason, othersText in
        hostingController?.dismiss(animated: true, completion: nil)
        guard let self = self else { return }
        Task { @MainActor in
          let reportReason = othersText.isEmpty ? reason : othersText
          await self.viewModel.report(request: ReportRequest(
            postId: UInt64(feedItem.postID) ?? .zero,
            videoId: feedItem.videoID,
            reason: reportReason,
            canisterID: feedItem.canisterID,
            principal: feedItem.principalID
          ))
        }
        self.getNudgeView(at: index, isDelete: false)
      },
      onDismiss: {
        hostingController?.dismiss(animated: true, completion: nil)
      }
    )

    hostingController = UIHostingController(rootView: reportView)
    hostingController!.modalPresentationStyle = .overFullScreen
    hostingController!.modalTransitionStyle = .crossDissolve
    hostingController?.view.backgroundColor = .clear
    self.present(hostingController!, animated: true, completion: nil)
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoClickedEventData(
        videoId: feedItem.videoID,
        publisherUserId: feedItem.principalID,
        likeCount: Int64(feedItem.likeCount),
        shareCount: .zero,
        viewCount: feedItem.viewCount,
        isGameEnabled: true,
        gameType: .smiley,
        isNsfw: false,
        ctaType: .report,
        pageName: self.feedType == .otherUsers ? .home : .profile
      )
    )
  }

  func videoStarted(index: Int, videoId: String) {
    let snapshot = feedsDataSource.snapshot()
    guard index < snapshot.itemIdentifiers.count, videoId == snapshot.itemIdentifiers[index].videoID else { return }
    let feedItem = snapshot.itemIdentifiers[index]
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: VideoStartedEventData(
        videoId: feedItem.videoID,
        publisherUserId: feedItem.principalID,
        likeCount: Int64(feedItem.likeCount),
        shareCount: .zero,
        viewCount: feedItem.viewCount,
        isGameEnabled: true,
        gameType: .smiley,
        isNsfw: false
      )
    )
  }

  func getNudgeView(at index: Int, isDelete: Bool) {
    let snapshot = feedsDataSource.snapshot()
    guard index < snapshot.itemIdentifiers.count else { return }
    let feedItem = snapshot.itemIdentifiers[index]

    var hostingController: UIHostingController<NudgePopupView>?
    let title = isDelete ? Constants.deleteNudgeTitle : Constants.blockNudgeTitle
    let subtitle = isDelete ? Constants.deleteNudgeSubtitle : Constants.blockNudgeSubtitle
    let confrimLabel = isDelete ? Constants.deleteConfirmButton : Constants.blockConfirmButton
    let cancelLabel = isDelete ? Constants.deleteCancelButton : Constants.blockCancelButton
    let nudgeView = NudgePopupView(
      nudgeTitle: title,
      nudgeMessage: subtitle,
      confirmLabel: confrimLabel,
      cancelLabel: cancelLabel,
      onConfirm: { [weak self] in
        hostingController?.dismiss(animated: true, completion: nil)
        guard let self = self else { return }
        Task { @MainActor in
          if isDelete {
            await self.viewModel.deleteVideo(
              request: DeleteVideoRequest(
                postId: UInt64(feedItem.postID) ?? .zero,
                videoId: feedItem.videoID
              )
            )
          } else {
            await self.viewModel.blockUser(principalId: feedItem.principalID)
          }
        }
      },
      onCancel: {
        hostingController?.dismiss(animated: true, completion: nil)
      }
    )

    hostingController = UIHostingController(rootView: nudgeView)
    hostingController!.modalPresentationStyle = .overFullScreen
    hostingController!.modalTransitionStyle = .crossDissolve
    hostingController?.view.backgroundColor = .clear
    self.present(hostingController!, animated: true, completion: nil)
  }
}

extension FeedsViewController: FeedsPlayerProtocol {
  func cacheCleared(atc index: Int) {
    guard index < feedsDataSource.snapshot().itemIdentifiers.count else { return }
    lastDisplayedThumbnailPath.removeValue(
      forKey: feedsDataSource.snapshot().itemIdentifiers[index].videoID
    )
  }

  func removeThumbnails(for set: Set<Int>) {
    for index in set {
      guard index < feedsDataSource.snapshot().itemIdentifiers.count else { continue }
      lastDisplayedThumbnailPath.removeValue(
        forKey: feedsDataSource.snapshot().itemIdentifiers[index].videoID
      )
    }
  }

  func reachedPlaybackMilestone(_ milestone: PlaybackMilestone, for index: Int) {
    guard let item = feedsPlayer.player.currentItem else { return }
    let duration = item.duration.seconds
    guard duration.isFinite, duration > 0 else { return }

    let absoluteWatched: Double
    let percentageWatched: Double
    switch milestone {
    case .started:
      absoluteWatched   = min(duration, 0.1)
      percentageWatched = absoluteWatched / duration * 100
    case .almostFinished:
      absoluteWatched   = duration * 0.95
      percentageWatched = 95
    }
    guard index < feedsDataSource.snapshot().itemIdentifiers.count else { return }
    let feed = feedsDataSource.snapshot().itemIdentifiers[index]

    Task { @MainActor in
      await viewModel.log(
        event: VideoEventRequest(
          displayName: feed.displayName,
          hashtagCount: Int32(feed.hashtags.count),
          isHotOrNot: feed.nsfwProbability > Constants.nsfwProbability,
          isLoggedIn: session.state.isLoggedIn,
          isNsfw: false,
          likeCount: Int32(feed.likeCount),
          nsfwProbability: feed.nsfwProbability,
          percentageWatched: percentageWatched,
          postID: Int32(feed.postID) ?? .zero,
          publisherCanisterID: feed.canisterID,
          publisherUserID: feed.principalID,
          videoDuration: duration,
          absoluteWatched: absoluteWatched,
          videoID: feed.videoID,
          viewCount: feed.viewCount,
          event: VideoEventType.durationWatched.rawValue
        )
      )
    }
  }

  func playedThreeSeconds(at index: Int) {
    if index < self.feedsDataSource.snapshot().numberOfItems {
      print("Sarvesh here")
      let item = self.feedsDataSource.snapshot().itemIdentifiers[index]
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: VideoViewedEventData(
          videoId: item.videoID,
          publisherUserId: item.principalID,
          likeCount: Int64(item.likeCount),
          shareCount: .zero,
          viewCount: item.viewCount,
          isGameEnabled: feedType == .otherUsers ? true : false,
          gameType: .smiley,
          isNsfw: false
        )
      )
    }
  }

  func loginTapped(provider: SocialProvider) {
    Task {
      await self.viewModel.socialSignIn(request: provider)
    }
  }
}
