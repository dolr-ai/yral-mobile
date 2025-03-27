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
    activityIndicator.startAnimating()
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
    let snapshot = feedsDataSource.snapshot()
    guard index < snapshot.itemIdentifiers.count else { return }
    let feedItem = snapshot.itemIdentifiers[index]

    var hostingController: UIHostingController<NudgePopupView>?

    let nudgeView = NudgePopupView(
      nudgeTitle: "Delete video?",
      nudgeMessage: "This video will be permanently deleted from your Yral account.",
      confirmLabel: "Delete",
      cancelLabel: "Cancel",
      onConfirm: { [weak self] in
        // Dismiss the popup first.
        hostingController?.dismiss(animated: true, completion: nil)
        guard let self = self else { return }
        Task { @MainActor in
          await self.viewModel.deleteVideo(
            request: DeleteVideoRequest(
              postId: UInt64(feedItem.postID) ?? .zero,
              videoId: feedItem.videoID
            )
          )
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

extension FeedsViewController: YralPlayerProtocol {
  func cacheCleared(atc index: Int) {
    lastDisplayedThumbnailPath.removeValue(forKey: index)
  }

  func removeThumbnails(for set: Set<Int>) {
    for index in set {
      lastDisplayedThumbnailPath.removeValue(forKey: index)
    }
  }
}
