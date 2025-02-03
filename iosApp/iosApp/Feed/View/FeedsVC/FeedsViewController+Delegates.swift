//
//  FeedsViewController+Delegates.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit

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
}
