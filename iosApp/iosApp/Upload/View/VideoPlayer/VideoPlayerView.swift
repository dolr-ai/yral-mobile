//
//  VideoPlayerView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit

struct VideoPlayerView: View {
  @Binding var player: AVPlayer?
  @Binding var isPlaying: Bool
  @Binding var showControls: Bool
  let url: URL

  var body: some View {
    VideoPlayer(player: player)
      .disabled(true)
      .background(UploadView.Constants.videoPlayerBGColor)
      .cornerRadius(UploadView.Constants.videoPlayerRadius)
      .overlay(
        RoundedRectangle(cornerRadius: UploadView.Constants.videoPlayerRadius)
          .inset(by: .half)
          .stroke(UploadView.Constants.videoPlayerStrokeColor, lineWidth: .one)
      )
      .onAppear {
        // Initialize the AVPlayer and observer
        if player == nil {
          player = AVPlayer(url: url)
          NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: player?.currentItem,
            queue: .main
          ) { _ in
            player?.seek(to: .zero)
            player?.play()
          }
        }
      }
      .onChange(of: url) { newURL in
        // Reset player to new URL
        player = AVPlayer(url: newURL)
        isPlaying = false
        NotificationCenter.default.addObserver(
          forName: .AVPlayerItemDidPlayToEndTime,
          object: player?.currentItem,
          queue: .main
        ) { _ in
          player?.seek(to: .zero)
          player?.play()
        }
      }
      .onDisappear {
        player?.pause()
        isPlaying = false
      }
      .onTapGesture {
        withAnimation {
          showControls.toggle()
        }
      }
  }
}
