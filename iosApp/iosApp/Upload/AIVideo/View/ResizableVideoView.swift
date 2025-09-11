//
//  ResizableVideoView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit
import Combine

struct ResizableVideoView: View {
  let url: URL
  var cornerRadius: CGFloat = 8.0
  var aspectRatio: CGFloat

  @State private var player: AVQueuePlayer?
  @State private var looper: AVPlayerLooper?
  @State private var cancellables = Set<AnyCancellable>()

  var body: some View {
    ZStack {
      if let player {
        VideoPlayer(player: player)
          .allowsHitTesting(false)
          .onAppear { player.play() }
          .onDisappear { player.pause() }
      }
    }
    .aspectRatio(aspectRatio, contentMode: .fit)
    .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    .task {
      cancellables.removeAll()

      let asset = AVURLAsset(url: url)
      let item  = AVPlayerItem(asset: asset)
      let queuePlayer = AVQueuePlayer(playerItem: item)
      queuePlayer.isMuted = false
      queuePlayer.actionAtItemEnd = .none

      let loop = AVPlayerLooper(player: queuePlayer, templateItem: item)

      self.player = queuePlayer
      self.looper = loop

      queuePlayer.play()
    }
  }
}
