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

  func likeButtonTapped(index: Int) {
    guard let postID = Int(feedsDataSource.snapshot().itemIdentifiers[index].postID) else { return }
    let canisterID = feedsDataSource.snapshot().itemIdentifiers[index].canisterID
    Task { @MainActor in
      await self.viewModel.toggleLike(request: LikeQuery(postID: postID, canisterID: canisterID, index: index))
    }
    guard let cell = feedsCV.cellForItem(at: IndexPath(item: index, section: .zero)) as? FeedsCell else { return }
    cell.setLikeStatus(isLiked: cell.likeButton.configuration?.image == FeedsCell.Constants.likeUnSelectedImage)
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
            canisterID: feedItem.canisterID
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
      lastDisplayedThumbnailPath.removeValue(
        forKey: feedsDataSource.snapshot().itemIdentifiers[index].videoID
      )
    }
  }
}
