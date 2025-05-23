//
//  FeedsViewController+Delegates.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import SwiftUI

extension FeedsViewController: FeedsCellProtocol {
  func makeSmileyGameRulesDIContainer() -> SmileyGameRuleDIContainer {
    return SmileyGameRuleDIContainer(
      dependencies: SmileyGameRuleDIContainer.Dependencies(
        firebaseService: FirebaseService(),
        crashReporter: CompositeCrashReporter(reporters: [FirebaseCrashlyticsReporter()])
      )
    )
  }

  func showGameResultBottomSheet(index: Int, gameResult: SmileyGameResultResponse) {
    var hostingController: UIHostingController<SmileyGameResultBottomSheetView>?
    let bottomSheetView = SmileyGameResultBottomSheetView(
      gameResult: gameResult) {
        hostingController?.dismiss(animated: true)
      } onLearnMoreTapped: {
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
  }


  func deleteButtonTapped(index: Int) {
    getNudgeView(at: index, isDelete: true)
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

  func loginTapped(provider: SocialProvider) {
    Task {
      await self.viewModel.socialSignIn(request: provider)
    }
  }
}
