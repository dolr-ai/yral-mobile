//
//  UploadView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit
import iosSharedUmbrella

struct UploadView: View {
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
  @EnvironmentObject var session: SessionManager
  @EnvironmentObject var eventBus: EventBus

  let onDismiss: () -> Void
  var isUploadEnabled: Bool {
    let isVideoURLValid = (videoURL != nil) && (videoURL?.absoluteString.isEmpty == false)
    return isVideoURLValid
  }

  init(viewModel: UploadViewModel, onDismiss: @escaping () -> Void) {
    self.viewModel = viewModel
    self.onDismiss = onDismiss
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
          HStack(alignment: .center, spacing: Constants.navHstackSpacing) {
            Image(Constants.backImage)
              .resizable()
              .frame(width: Constants.backImageSize, height: Constants.backImageSize)
              .onTapGesture {
                onDismiss()
              }

            Text(Constants.navigationTitle)
              .font(Constants.navigationTitleFont)
              .foregroundColor(Constants.navigationTitleTextColor)
              .frame(maxWidth: .infinity, alignment: .leading)
          }
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
        VStack(spacing: Constants.verticalSpacing) {
          HStack(alignment: .center, spacing: Constants.navHstackSpacing) {
            Image(Constants.backImage)
              .resizable()
              .frame(width: Constants.backImageSize, height: Constants.backImageSize)
              .onTapGesture {
                onDismiss()
              }

            Text(Constants.navigationTitle)
              .font(Constants.navigationTitleFont)
              .foregroundColor(Constants.navigationTitleTextColor)
              .frame(maxWidth: .infinity, alignment: .leading)
          }
          .padding(Constants.navigationTitlePadding)
          if let url = videoURL {
            ZStack {
              VideoPlayerView(
                player: $player,
                isPlaying: $isPlaying,
                showControls: $showControls,
                url: url
              )
              .frame(maxWidth: .infinity, maxHeight: .infinity)
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
          .padding(.bottom, Constants.uploadButtonBottomPadding)
        }
        .padding(.horizontal, Constants.horizontalPadding)
        .fullScreenCover(isPresented: $showVideoPicker) {
          VideoPickerViewControllerRepresentable(viewModel: viewModel)
            .background( ClearBackgroundView() )
        }
      }
    }
    .animation(.easeInOut, value: showUploadCompletedView)
    .animation(.easeInOut, value: showUploadFailedView)
    .ignoresSafeArea(.container, edges: .bottom)
    .padding(.bottom, Constants.bottomAdjustmentYralTabBat)
    .onReceive(viewModel.$event) { event in
      guard let event = event else { return }
      switch event {
      case .uploadProgressUpdated(let progress):
        currentProgress = progress

      case .uploadPressed:
        showUploadProgressView = true
        guard let url = videoURL else { return }
        Task {
          await viewModel.finishUpload(fileURL: url, caption: "", hashtags: [""])
        }
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: VideoUploadInitiatedEventData(captionAdded: false, hashtagsAdded: false, type: .uploadVideo)
        )

      case .videoSelected(let url):
        videoURL = url
        viewModel.startUpload(fileURL: url, caption: "", hashtags: [""])
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: FileSelectionSuccessEventData(fileType: "video")
        )

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
          resetUploadScreen()
          onDismiss()
          eventBus.finishUploadingVideo.send(())
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
          resetUploadScreen()
        },
        goHomeAction: {
          resetUploadScreen()
          onDismiss()
          eventBus.finishUploadingVideo.send(())
        }
      )
      .transition(.opacity)
    }
    .task {
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(
        event: VideoCreationPageViewedEventData(
          type: .uploadVideo,
          creditsFetched: nil,
          creditsAvailable: nil
        )
      )
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

    currentProgress = .zero
    viewModel.state = .initialized
  }
}

extension UploadView {
  enum Constants {
    static let navHstackSpacing = 12.0
    static let backImage = "chevron-left"
    static let backImageSize = 24.0
    static let uploadProgessVStackSpacing: CGFloat = 30.0
    static let navigationTitle = "Upload Video"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let navigationTitlePadding = EdgeInsets(top: 24.0, leading: 0, bottom: 0, trailing: 0)
    static let verticalSpacing = 20.0
    static let horizontalPadding = 16.0
    static let uploadButtonBottomPadding = 16.0

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
    static let bottomAdjustmentYralTabBat = 18.0
    static let uploadButtonTitle = "Upload"
    static let uploadButtonTextColor = YralColor.grey50.swiftUIColor
    static let uploadButtonFont = YralFont.pt16.bold.swiftUIFont
    static let uploadButtonHeight = 45.0
    static let uploadButtonRadius = 8.0

    static let videoPlayerHeight = 300.0
    static let videoPlayerRadius = 8.0
    static let videoPlayerBGColor = YralColor.grey950.swiftUIColor
    static let videoPlayerStrokeColor = YralColor.grey800.swiftUIColor
    static let closeButtonPadding = EdgeInsets(top: 10, leading: 0, bottom: 0, trailing: 10)
    static let uploadPlayerCloseImageName = "upload_player_close"
    static let captionsViewId = "CaptionsView"
    static let hashtagsViewId = "HashtagView"
    static let playPauseButtonSize: CGFloat = 64.0
    static let playImageName = "video_play"
    static let pauseImageName = "video_pause"
  }
}
