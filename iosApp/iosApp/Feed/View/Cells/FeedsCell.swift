//
//  FeedsCell.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit
import AVFoundation

class FeedsCell: UICollectionViewCell, ReusableView, ImageLoaderProtocol {

  var playerLayer: AVPlayerLayer?
  private let playerContainerView = getUIImageView()

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupUI()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupUI() {
    contentView.addSubview(playerContainerView)
    NSLayoutConstraint.activate([
      playerContainerView.topAnchor.constraint(equalTo: contentView.topAnchor),
      playerContainerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
      playerContainerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
      playerContainerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor)
    ])
    contentView.layoutIfNeeded()
  }

  func configure(withPlayer player: AVPlayer, thumbnailURL: URL, lastFrameImage: UIImage?) {
    if let lastFrameImage {
      playerContainerView.image = lastFrameImage
    } else {
      loadImage(with: thumbnailURL, on: playerContainerView)
    }
    playerLayer?.removeFromSuperlayer()
    let layer = AVPlayerLayer(player: player)
    layer.videoGravity = .resize
    playerContainerView.layer.addSublayer(layer)
    playerLayer = layer
    playerLayer?.frame = contentView.bounds
  }

  override func layoutSubviews() {
    playerLayer?.frame = playerContainerView.bounds
  }

  override func prepareForReuse() {
      super.prepareForReuse()
      playerLayer?.player = nil
  }
}
