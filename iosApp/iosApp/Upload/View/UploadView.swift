//
//  UploadView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit

struct UploadView: View {
  @State private var caption: String = ""
  @State private var hashtags = [HashtagItem]()
  @State private var showVideoPicker = false
  @State private var videoURL: URL?
  @State private var player: AVPlayer?
  @State private var isPlaying: Bool = false
  @State private var showControls: Bool = true
  @State private var showUploadProgressView = false
  @State private var currentProgress: Double = 0.0
  @State private var showUploadCompletedView = false
  @State private var showUploadFailedView = false
  @ObservedObject var viewModel: UploadViewModel

  var doneAction: () -> Void = {}
  var isUploadEnabled: Bool {
    let isCaptionEmpty = caption.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    let areHashtagsEmpty = hashtags.isEmpty
    let isVideoURLValid = (videoURL != nil) && (videoURL?.absoluteString.isEmpty == false)
    return !isCaptionEmpty && !areHashtagsEmpty && isVideoURLValid
  }

  init(viewModel: UploadViewModel) {
    self.viewModel = viewModel
    // swiftlint: disable unavailable_condition
    if #available(iOS 16.0, *) {
      // No extra setup needed
    } else {
      UITextView.appearance().backgroundColor = .clear
    }
    // swiftlint: enable unavailable_condition
  }

  var body: some View {
    ZStack {
      if showUploadProgressView, let url = videoURL {
        VStack(spacing: .zero) {
          Text(Constants.navigationTitle)
            .font(Constants.navigationTitleFont)
            .foregroundColor(Constants.navigationTitleTextColor)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Constants.navigationTitlePadding)

          VStack(spacing: Constants.uploadProgessVStackSpacing) {
            UploadProgressView(progressValue: $currentProgress, videoURL: url)
            ZStack {
              VideoPlayerView(
                player: $player,
                isPlaying: $isPlaying,
                showControls: $showControls,
                url: url
              )
              .onAppear {
                player?.play()
                isPlaying = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                  withAnimation {
                    showControls = false
                  }
                }
              }
              if showControls {
                Button {
                  togglePlayback()
                } label: {
                  Image(isPlaying ? Constants.pauseImageName : Constants.playImageName)
                    .resizable()
                    .frame(width: Constants.playPauseButtonSize, height: Constants.playPauseButtonSize)
                    .foregroundColor(.white)
                }
                .buttonStyle(PlainButtonStyle())
              }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
          }
        }
        .padding(.horizontal, Constants.horizontalPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.edgesIgnoringSafeArea(.all))
        .transition(.opacity)
      } else {
        ScrollViewReader { proxy in
          ScrollView {
            VStack(spacing: Constants.verticalSpacing) {
              Text(Constants.navigationTitle)
                .font(Constants.navigationTitleFont)
                .foregroundColor(Constants.navigationTitleTextColor)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(Constants.navigationTitlePadding)

              if let url = videoURL {
                ZStack {
                  VideoPlayerView(
                    player: $player,
                    isPlaying: $isPlaying,
                    showControls: $showControls,
                    url: url
                  )
                  .frame(height: Constants.videoPlayerHeight)

                  if showControls {
                    Button {
                      togglePlayback()
                    } label: {
                      Image(isPlaying ? Constants.pauseImageName : Constants.playImageName)
                        .resizable()
                        .frame(width: Constants.playPauseButtonSize, height: Constants.playPauseButtonSize)
                        .foregroundColor(.white)
                    }
                    .buttonStyle(PlainButtonStyle())
                  }
                }
                .overlay(
                  Button {
                    viewModel.cancelUpload()
                  } label: {
                    Image(Constants.uploadPlayerCloseImageName)
                  }
                    .padding(Constants.closeButtonPadding),
                  alignment: .topTrailing
                )
              } else {
                SelectFileView(showVideoPicker: $showVideoPicker)
              }

              CaptionsView(caption: $caption, onFocus: {
                withAnimation {
                  proxy.scrollTo(Constants.captionsViewId, anchor: .center)
                }
              })
              .id(Constants.captionsViewId)

              HashtagView(hashtags: $hashtags) {
                withAnimation {
                  proxy.scrollTo(Constants.hashtagsViewId, anchor: .center)
                }
              }
              .id(Constants.hashtagsViewId)

              Button {
                viewModel.event = .uploadPressed
              } label: {
                Text(Constants.uploadButtonTitle)
                  .foregroundColor(Constants.uploadButtonTextColor)
                  .font(Constants.uploadButtonFont)
                  .frame(maxWidth: .infinity, minHeight: Constants.uploadButtonHeight)
                  .background(
                    isUploadEnabled
                    ? Constants.uploadButtonEnabledGradient
                    : Constants.uploadButtonDisabledGradient
                  )
                  .cornerRadius(Constants.uploadButtonRadius)
              }
              .disabled(!isUploadEnabled)
            }
            .padding(.horizontal, Constants.horizontalPadding)
          }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .hideKeyboardOnTap()
          .fullScreenCover(isPresented: $showVideoPicker) {
            if #available(iOS 16.4, *) {
              VideoPickerViewControllerRepresentable(viewModel: viewModel)
                .presentationDetents([])
                .presentationDragIndicator(.hidden)
                .presentationBackground(.clear)
            } else {
              VideoPickerViewControllerRepresentable(viewModel: viewModel)
            }
          }
        }
      }
    }
    .animation(.easeInOut, value: showUploadCompletedView)
    .animation(.easeInOut, value: showUploadFailedView)
    .onAppear {
      Task {
        await viewModel.getUploadEndpoint()
      }
    }
    .onReceive(viewModel.$event) { event in
      guard let event = event else { return }
      switch event {
      case .uploadProgressUpdated(let progress):
        currentProgress = progress

      case .uploadPressed:
        showUploadProgressView = true
        guard let url = videoURL else { return }
        Task {
          await viewModel.finishUpload(fileURL: url, caption: caption, hashtags: hashtags.map { $0.text })
        }

      case .videoSelected(let url):
        videoURL = url
        viewModel.startUpload(fileURL: url, caption: caption, hashtags: hashtags.map { $0.text })

      case .videoUploadSuccess:
        UIView.setAnimationsEnabled(false)
        showUploadCompletedView = true

      case .videoUploadFailure(let error):
        UIView.setAnimationsEnabled(false)
        showUploadFailedView = true
        print(error)
      case .videoUploadCancelled:
        videoURL = nil
        player = nil
        isPlaying = false
        showControls = true
      }
      viewModel.event = nil
    }
    .fullScreenCover(
      isPresented: Binding<Bool>(
        get: { showUploadCompletedView && showUploadProgressView },
        set: { _ in }
      )
    ) {
      UploadCompletedView(
        doneAction: {
          Task {
            await viewModel.getUploadEndpoint()
          }
          resetUploadScreen()
          doneAction()
        },
        showUploadCompletedView: $showUploadCompletedView
      )
      .transition(.opacity)
    }
    .fullScreenCover(
      isPresented: Binding<Bool>(
        get: { showUploadFailedView && showUploadProgressView },
        set: { _ in }
      )
    ) {
      UploadErrorView(
        showUploadFailedView: $showUploadFailedView,
        tryAgainAction: {
          Task {
            await viewModel.getUploadEndpoint()
          }
          resetUploadScreen()
        },
        goHomeAction: {
          Task {
            await viewModel.getUploadEndpoint()
          }
          resetUploadScreen()
          doneAction()
        }
      )
      .transition(.opacity)
    }
  }
}

extension UploadView {
  fileprivate func togglePlayback() {
    if isPlaying {
      player?.pause()
      isPlaying = false
    } else {
      player?.play()
      isPlaying = true
      DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
        withAnimation {
          showControls = false
        }
      }
    }
  }

  fileprivate func resetUploadScreen() {
    showUploadCompletedView = false
    showUploadFailedView = false
    showUploadProgressView = false

    videoURL = nil
    player = nil
    isPlaying = false
    showControls = true

    caption = ""
    hashtags = []

    currentProgress = .zero
  }

  func onDoneAction(_ action: @escaping () -> Void) -> UploadView {
    var copy = self
    copy.doneAction = action
    return copy
  }
}

extension UploadView {
  enum Constants {
    static let uploadProgessVStackSpacing: CGFloat = 30.0
    static let navigationTitle = "Upload Video"
    static let navigationTitleFont = Font.custom("Kumbh Sans", size: 20).weight(.bold)
    static let navigationTitleTextColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let navigationTitlePadding = EdgeInsets(top: 24.0, leading: 0, bottom: 0, trailing: 0)
    static let verticalSpacing = 20.0
    static let horizontalPadding = 16.0

    static let uploadButtonEnabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 1, green: 0.47, blue: 0.76), location: 0.00),
        Gradient.Stop(color: Color(red: 0.89, green: 0, blue: 0.48), location: 0.51),
        Gradient.Stop(color: Color(red: 0.68, green: 0, blue: 0.37), location: 1.00)
      ],
      startPoint: UnitPoint(x: 0.94, y: 0.13),
      endPoint: UnitPoint(x: 0.35, y: 0.89)
    )
    static let uploadButtonDisabledGradient = LinearGradient(
      stops: [
        Gradient.Stop(color: Color(red: 0.87, green: 0.6, blue: 0.75), location: 0.00),
        Gradient.Stop(color: Color(red: 0.77, green: 0.36, blue: 0.58), location: 0.33),
        Gradient.Stop(color: Color(red: 0.51, green: 0.33, blue: 0.43), location: 1.00)
      ],
      startPoint: UnitPoint(x: 1, y: 0.51),
      endPoint: UnitPoint(x: 0.03, y: 1)
    )
    static let uploadButtonTitle = "Upload"
    static let uploadButtonTextColor = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let uploadButtonFont = Font.custom("Kumbh Sans", size: 16).weight(.bold)
    static let uploadButtonHeight = 45.0
    static let uploadButtonRadius = 8.0

    static let videoPlayerHeight = 300.0
    static let videoPlayerRadius = 8.0
    static let videoPlayerBGColor = Color(red: 0.04, green: 0.04, blue: 0.04)
    static let videoPlayerStrokeColor = Color(red: 0.13, green: 0.13, blue: 0.13)
    static let closeButtonPadding = EdgeInsets(top: 10, leading: 0, bottom: 0, trailing: 10)
    static let uploadPlayerCloseImageName = "upload_player_close"
    static let captionsViewId = "CaptionsView"
    static let hashtagsViewId = "HashtagView"
    static let playPauseButtonSize: CGFloat = 64.0
    static let playImageName = "video_play"
    static let pauseImageName = "video_pause"
  }
}
