//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileView: View {
  @State var showAccountInfo = false
  @State var showEmptyState = true
  @State var showDelete = false
  @State private var isLoadingFirstTime = true
  @State var accountInfo: AccountInfo?
  @State var videos: [ProfileVideoInfo] = []
  @State private var deleteInfo: ProfileVideoInfo?
  @State private var showDeleteIndicator: Bool = false
  @State private var showFeeds = false
  @State private var currentIndex: Int = .zero
  var uploadVideoPressed: (() -> Void) = {}

  let viewModel: ProfileViewModel
  let router: ProfileRouterProtocol

  init(viewModel: ProfileViewModel, router: ProfileRouterProtocol) {
    self.viewModel = viewModel
    self.router = router
  }

  var body: some View {
    if showFeeds {
      router.displayUserVideoFeed(
        existingFeeds: viewModel.feeds,
        info: MyVideosFeedInfo(
          startIndex: viewModel.startIndex,
          currentIndex: currentIndex
        ),
        showFeeds: $showFeeds
      )
      .edgesIgnoringSafeArea(.all)
    } else {
      ScrollViewReader { _ in
        ScrollView {
          VStack(spacing: Constants.vStackSpacing) {
            Text(Constants.navigationTitle)
              .font(Constants.navigationTitleFont)
              .foregroundColor(Constants.navigationTitleTextColor)
              .frame(maxWidth: .infinity, alignment: .leading)
              .padding(Constants.navigationTitlePadding)

            if showAccountInfo {
              UserInfoView(accountInfo: $accountInfo, shouldApplySpacing: false)
            }
            if showEmptyState {
              Group {
                Spacer(minLength: Constants.minimumTopSpacing)
                ProfileEmptyStateView {
                  uploadVideoPressed()
                }
                Spacer(minLength: Constants.minimumBottomSpacing)
              }
            } else {
              ProfileVideosGridView(
                videos: $videos,
                currentlyDeletingPostInfo: $deleteInfo,
                showDeleteIndictor: $showDeleteIndicator,
                onDelete: { info in
                  self.deleteInfo = info
                  withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
                    UIView.setAnimationsEnabled(false)
                    showDelete = true
                  }
                },
                onVideoTapped: { videoInfo in
                  currentIndex = videos.firstIndex(where: { $0.postID == videoInfo.postID }) ?? .zero
                  withAnimation {
                    showFeeds = true
                  }
                },
                onLoadMore: {
                  Task { @MainActor in
                    await viewModel.getVideos()
                  }
                }
              )
            }
          }
          .padding(.horizontal, Constants.horizontalPadding)
        }
        .fullScreenCover(isPresented: $showDelete) {
          NudgePopupView(
            nudgeTitle: Constants.deleteTitle,
            nudgeMessage: Constants.deleteText,
            confirmLabel: Constants.deleteButtonTitle,
            cancelLabel: Constants.cancelTitle,
            onConfirm: {
              showDelete = false
              showDeleteIndicator = true
              Task { @MainActor in
                guard let deleteInfo else { return }
                await self.viewModel.deleteVideo(
                  request: DeleteVideoRequest(
                    postId: UInt64(deleteInfo.postID) ?? .zero,
                    videoId: deleteInfo.videoId
                  )
                )
              }
            },
            onCancel: { showDelete = false }
          )
          .background( ClearBackgroundView() )
        }
        .onReceive(viewModel.$event) { event in
          switch event {
          case .fetchedAccountInfo(let info):
            showAccountInfo = true
            accountInfo = info
          case .loadedVideos(let videos):
            guard !videos.isEmpty else { return }
            showEmptyState = false
            self.videos += videos
          case .deletedVideos:
            withAnimation {
              self.viewModel.deletedVideos.forEach { item in
                self.videos.removeAll { $0.postID == item.postID }
              }
            }
            if self.videos.isEmpty {
              showEmptyState = true
            }
            self.showDeleteIndicator = false
          case .deleteVideoFailed:
            self.deleteInfo = nil
            self.showDeleteIndicator = false
          default:
            break
          }
        }
        .task {
          guard isLoadingFirstTime else { return }
          isLoadingFirstTime = false
          async let fetchProfile: () = viewModel.fetchProfileInfo()
          async let fetchVideos: () = viewModel.getVideos()
          _ = await (fetchProfile, fetchVideos)
        }
      }
    }
  }

  func onUploadAction(_ action: @escaping () -> Void) -> ProfileView {
    var copy = self
    copy.uploadVideoPressed = action
    return copy
  }
}

extension ProfileView {
  enum Constants {
    static let navigationTitle = "My Profile"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let navigationTitlePadding = EdgeInsets(
      top: 20.0,
      leading: 0.0,
      bottom: 16.0,
      trailing: 0.0
    )

    static let vStackSpacing: CGFloat = 20.0
    static let horizontalPadding: CGFloat = 16.0
    static let minimumTopSpacing: CGFloat = 16.0
    static let minimumBottomSpacing: CGFloat = 16.0

    static let deleteTitle = "Delete video?"
    static let deleteText = "This video will be permanently deleted from your Yral account."
    static let cancelTitle = "Cancel"
    static let deleteButtonTitle = "Delete"
  }
}
