//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

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
  @EnvironmentObject var session: SessionManager
  var uploadVideoPressed: (() -> Void) = {}

  @StateObject var viewModel: ProfileViewModel
  let router: ProfileRouterProtocol

  init(viewModel: ProfileViewModel, router: ProfileRouterProtocol) {
    self._viewModel = StateObject(wrappedValue: viewModel)
    self.router = router
  }

  var body: some View {
    Group {
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
        VStack(spacing: .zero) {
          VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
            Text(Constants.navigationTitle)
              .font(Constants.navigationTitleFont)
              .foregroundColor(Constants.navigationTitleTextColor)
              .padding(Constants.navigationTitlePadding)
            if showAccountInfo {
              UserInfoView(
                accountInfo: $accountInfo,
                shouldApplySpacing: false,
                showLoginButton: Binding(get: { false }, set: { _ in }),
                delegate: nil
              )
            }
          }
          .padding(.horizontal, Constants.horizontalPadding)

          ScrollViewReader { _ in
            ScrollView {
              Group {
                if showEmptyState {
                  VStack {
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
            .offset(y: 20.0)
            .refreshable {
              Task {
                await self.viewModel.refreshVideos(request: RefreshVideosRequest(shouldPurge: false))
              }
            }
          }
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
        .task {
          guard isLoadingFirstTime else {
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: ProfilePageViewedEventData(totalVideos: Int32(self.videos.count))
            )
            return
          }
          isLoadingFirstTime = false
          async let fetchProfile: () = viewModel.fetchProfileInfo()
          async let fetchVideos: () = viewModel.getVideos()
          _ = await (fetchProfile, fetchVideos)
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: ProfilePageViewedEventData(totalVideos: Int32(self.videos.count))
          )
        }
        .onAppear {
          UIRefreshControl.appearance().tintColor = .clear
          UIRefreshControl.appearance().addSubview(LottieRefreshSingletonView.shared)
        }
      }
    }
    .onChange(of: viewModel.event) { event in
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
      case .refreshed(let videos):
        self.videos = videos
        showEmptyState = self.videos.isEmpty
      case .pageEndReached(let isEmpty):
        showEmptyState = isEmpty
      default:
        break
      }
    }
    .onChange(of: session.state) { state in
      switch state {
      case .loggedOut,
          .ephemeralAuthentication,
          .permanentAuthentication:
        Task {
          await viewModel.fetchProfileInfo()
          await viewModel.refreshVideos(
            request: RefreshVideosRequest(
              shouldPurge: true
            )
          )
        }
      default: break
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
