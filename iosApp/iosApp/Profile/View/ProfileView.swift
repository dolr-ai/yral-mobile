//
//  ProfileView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 16/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella
import Combine

// swiftlint: disable type_body_length file_length
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
  @State private var showAccount = false
  @State private var showEditProfile = false
  @State private var currentIndex: Int = .zero
  @State private var isPushNotificationFlow: Bool = false
  @State private var isVisible = false
  @State private var showLoginButton: Bool = false
  @State private var walletPhase: WalletPhase = .none
  @State private var walletOutcome: WalletPhase = .none
  @State private var showSignupSheet: Bool = false
  @State private var showSignupFailureSheet: Bool = false
  @State private var loadingProvider: SocialProvider?

  @State private var showVideoInsights = false
  @State private var insightsVideoInfo: ProfileVideoInfo?

  @EnvironmentObject var session: SessionManager
  @EnvironmentObject private var deepLinkRouter: DeepLinkRouter
  @EnvironmentObject var eventBus: EventBus

  var uploadVideoPressed: (() -> Void) = {}

  @StateObject var viewModel: ProfileViewModel

  let router: ProfileRouterProtocol

  init(viewModel: ProfileViewModel, router: ProfileRouterProtocol) {
    self._viewModel = StateObject(wrappedValue: viewModel)
    self.router = router
    UITabBar.appearance().isTranslucent = false
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
          showFeeds: $showFeeds,
          walletPhase: $walletPhase,
          walletOutcome: $walletOutcome
        )
        .edgesIgnoringSafeArea(.all)
      } else if showAccount {
        router.displayAccountView(showAccount: $showAccount)
      } else if showEditProfile {
        if let unwrappedAccountInfo = accountInfo {
          router.displayEditProfileView(
            showEditProfile: $showEditProfile,
            accountInfo: Binding<AccountInfo>(
              get: { unwrappedAccountInfo },
              set: { newValue in
                self.accountInfo = newValue
              }
            )
          )
        }
      } else {
        VStack(spacing: .zero) {
          VStack(alignment: .leading, spacing: Constants.vStackSpacing) {
            HStack {
              Text(Constants.navigationTitle)
                .font(Constants.navigationTitleFont)
                .foregroundColor(Constants.navigationTitleTextColor)
                .padding(Constants.navigationTitlePadding)
              Spacer()
              Button { showAccount = true } label: {
                Image(Constants.profileMenuImageName)
                  .resizable()
                  .frame(width: Constants.profileMenuImageSize, height: Constants.profileMenuImageSize)
              }
            }
            if showAccountInfo {
              UserInfoView(
                accountInfo: $accountInfo,
                shouldApplySpacing: false,
                showLoginButton: $showLoginButton,
                showEditProfileButton: $showLoginButton.inverted,
                delegate: self
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
                        currentIndex = videos.firstIndex(where: { $0.postID == info.postID }) ?? .zero
                        if currentIndex < videos.count {
                          let item = viewModel.feeds[currentIndex]
                          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
                            event: VideoClickedEventData(
                              videoId: item.videoID,
                              publisherUserId: item.principalID,
                              likeCount: Int64(item.likeCount),
                              shareCount: Int64(Int.zero),
                              viewCount: item.viewCount,
                              isGameEnabled: false,
                              gameType: GameType.smiley,
                              isNsfw: false,
                              ctaType: .delete_,
                              pageName: .profile
                            )
                          )
                        }
                      }
                    },
                    onVideoTapped: { videoInfo in
                      currentIndex = videos.firstIndex(where: { $0.postID == videoInfo.postID }) ?? .zero
                      if currentIndex < viewModel.feeds.count {
                        let item = viewModel.feeds[currentIndex]
                        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
                          event: VideoClickedEventData(
                            videoId: item.videoID,
                            publisherUserId: item.principalID,
                            likeCount: Int64(item.likeCount),
                            shareCount: .zero,
                            viewCount: Int64(item.viewCount),
                            isGameEnabled: false,
                            gameType: .smiley,
                            isNsfw: false,
                            ctaType: .play,
                            pageName: .profile
                          )
                        )
                      }
                      withAnimation {
                        showFeeds = true
                      }
                    },
                    onInsightsTapped: { videoInfo in
                      insightsVideoInfo = videoInfo
                      showVideoInsights = true
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
                await self.refreshVideos(shouldPurge: false)
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
                AnalyticsModuleKt.getAnalyticsManager().trackEvent(
                  event: DeleteVideoInitiatedEventData(
                    pageName: .profile,
                    videoId: deleteInfo.videoId
                  )
                )
                await self.viewModel.deleteVideo(
                  request: DeleteVideoRequest(
                    postId: deleteInfo.postID,
                    videoId: deleteInfo.videoId
                  )
                )
              }
            },
            onCancel: { showDelete = false }
          )
          .background(ClearBackgroundView())
        }
        .task {
          guard isLoadingFirstTime else {
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(
              event: ProfilePageViewedEventData(
                totalVideos: Int32(self.videos.count),
                isOwnProfile: true,
                publisherUserId: accountInfo?.canisterID ?? ""
              )
            )
            return
          }
          isLoadingFirstTime = false
          async let fetchProfile: () = viewModel.fetchProfileInfo()
          async let fetchVideos: () = viewModel.getVideos()
          _ = await (fetchProfile, fetchVideos)
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: ProfilePageViewedEventData(
              totalVideos: Int32(self.videos.count),
              isOwnProfile: true,
              publisherUserId: accountInfo?.canisterID ?? ""
            )
          )
        }
        .onAppear {
          isVisible = true
          UIRefreshControl.appearance().tintColor = .clear
          UIRefreshControl.appearance().addSubview(LottieRefreshSingletonView.shared)
        }
        .onDisappear {
          isVisible = false
        }
      }
    }
    .ignoresSafeArea(.container, edges: .bottom)
    .padding(.bottom, Constants.bottomAdjustmentYralTabBar)
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
        self.sendAnalyticsInfo()
        AnalyticsModuleKt.getAnalyticsManager().trackEvent(
          event: VideoDeletedEventData(
            pageName: .profile,
            videoId: self.deleteInfo?.videoId ?? "",
            ctaType: .profileThumbnail
          )
        )
        self.showDeleteIndicator = false
      case .deleteVideoFailed:
        self.deleteInfo = nil
        self.showDeleteIndicator = false
      case .refreshed(let refreshVideos):
        if !refreshVideos.isEmpty {
          self.videos = refreshVideos
        }
        showEmptyState = self.videos.isEmpty
        if isPushNotificationFlow {
          showFeeds = true
          isPushNotificationFlow = false
        }
        if !isLoadingFirstTime, isVisible {
          sendAnalyticsInfo()
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: ProfilePageViewedEventData(
              totalVideos: Int32(self.videos.count),
              isOwnProfile: true,
              publisherUserId: accountInfo?.canisterID ?? ""
            )
          )
        }
      case .pageEndReached(let isEmpty):
        showEmptyState = isEmpty
      case .socialSignInSuccess:
        loadingProvider = nil
        showSignupSheet = false
      case .socialSignInFailure:
        if let authJourney = loadingProvider?.authJourney() {
          let affiliate = AppDIHelper().getAffiliateAttributionStore().peek()
          AnalyticsModuleKt.getAnalyticsManager().trackEvent(
            event: AuthFailedEventData(
              authJourney: authJourney,
              affiliate: affiliate
            )
          )
        }
        loadingProvider = nil
        showSignupSheet = false
        showSignupFailureSheet = true
      default:
        break
      }
      viewModel.event = nil
    }
    .onReceive(session.phasePublisher) { phase in
      switch phase {
      case .loggedOut, .ephemeral:
        self.showLoginButton = true
        Task {
          await viewModel.fetchProfileInfo()
          await refreshVideos(shouldPurge: true)
        }
      case .permanent:
        self.showLoginButton = false
        Task {
          await viewModel.fetchProfileInfo()
          await refreshVideos(shouldPurge: true)
        }
      default: break
      }
    }
    .onReceive(eventBus.updatedUsername) { _ in
      Task {
        await viewModel.fetchProfileInfo()
      }
    }
    .onReceive(eventBus.dismissFullScreenCoverProfile) { _ in
      showVideoInsights = false
      insightsVideoInfo = nil
    }
    .fullScreenCover(isPresented: $showSignupSheet) {
      ZStack(alignment: .center) {
        SignupSheet(
          onComplete: { showSignupSheet = false },
          loadingProvider: $loadingProvider,
          delegate: self
        )
        .background( ClearBackgroundView() )
      }
    }
    .fullScreenCover(isPresented: $showSignupFailureSheet) {
      SignupFailureSheet(onComplete: {
        showSignupSheet = false
        showSignupFailureSheet = false
      })
      .background( ClearBackgroundView() )
    }
    .overlay(alignment: .center, content: {
      if showVideoInsights {
        Color.black.opacity(0.8)
          .ignoresSafeArea()
          .transition(.opacity)
      }
    })
    .fullScreenCover(isPresented: Binding(
      get: { showVideoInsights && insightsVideoInfo != nil },
      set: { newValue in
        showVideoInsights = newValue
        if !newValue {
          insightsVideoInfo = nil
        }
      }
    )) {
      if let videoInfo = insightsVideoInfo {
        router.displayVideoInsightsBottomSheet(
          openedFromFeed: false,
          videoInfo: videoInfo) { updatedTotalViews in
            if let totalViews = updatedTotalViews,
               let currentIndex = videos.firstIndex(where: { $0.videoId == videoInfo.videoId }) {
              videos[currentIndex].viewCount = totalViews
            }
            showVideoInsights = false
            insightsVideoInfo = nil
          }
          .background(ClearBackgroundView())
      }
    }
    .onReceive(deepLinkRouter.$pendingDestination.compactMap { $0 }) { dest in
      guard dest == .profileAfterUpload else { return }
      isPushNotificationFlow = true
      Task { await refreshVideos(shouldPurge: false) }
    }
  }

  // MARK: - Helpers
  func onUploadAction(_ action: @escaping () -> Void) -> ProfileView {
    var copy = self
    copy.uploadVideoPressed = action
    return copy
  }

  func refreshVideos(shouldPurge: Bool) async {
    await self.viewModel.refreshVideos(
      request: RefreshVideosRequest(shouldPurge: shouldPurge)
    )
  }
}

extension ProfileView {
  func sendAnalyticsInfo() {
    // swiftlint: disable large_tuple
    let (userPrincipal, canisterPrincipal, email, coins, isLoggedIn): (String, String, String?, UInt64, Bool) = {
      switch session.state {
      case .ephemeralAuthentication(let userPrincipal, let canisterPrincipal, let coins, _, _):
        return (userPrincipal, canisterPrincipal, nil, coins, false)
      case .permanentAuthentication(let userPrincipal, let canisterPrincipal, let email, let coins, _, _):
        return (userPrincipal, canisterPrincipal, email, coins, true)
      default:
        return ("", "", nil, .zero, false)
      }
    }()
    // swiftlint: enable large_tuple
    viewModel.setAnalyticsInfo(
      analyticsInfo: ProfileViewModel.AnalyticsInfo(
        userPrincipal: userPrincipal,
        canisterPrincipal: canisterPrincipal,
        isLoggedIn: isLoggedIn,
        walletBalance: Double(coins),
        playToScroll: AppDIHelper().getFeatureFlagManager().isEnabled(
          flag: FeedFeatureFlags.SmileyGame.shared.StopAndVoteNudge
        ),
        emailId: email
      )
    )
  }
}

extension ProfileView: UserInfoViewProtocol {
  func loginPressed() {
    UIView.setAnimationsEnabled(false)
    showSignupSheet = true
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: AuthScreenViewedEventData(pageName: .menu)
    )
  }

  func editProfilePressed() {
    UIView.setAnimationsEnabled(false)
    showEditProfile = true
  }
}

extension ProfileView: SignupSheetProtocol {
  func signupwithGoogle() {
    loadingProvider = .google
    Task {
      await viewModel.socialSignIn(request: .google)
    }
  }

  func signupwithApple() {
    loadingProvider = .apple
    Task {
      await viewModel.socialSignIn(request: .apple)
    }
  }
}
// swiftlint: enable type_body_length file_length
