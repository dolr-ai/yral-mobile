import SwiftUI
import iosSharedUmbrella

struct HomeTabController: View {
  @EnvironmentObject var session: SessionManager
  @EnvironmentObject var deepLinkRouter: DeepLinkRouter
  @State private var suppressAnalytics = false
  let feedsViewController: FeedsViewController
  let uploadOptionsScreenView: UINavigationController
  let profileView: ProfileView
  let leaderboardView: UINavigationController
  let walletView: WalletView

  private var feedsViewControllerWrapper: FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(
      feedsViewController: feedsViewController,
      showFeeds: $showFeeds,
      walletPhase: $walletPhase,
      walletOutcome: $walletOutcome
    )
  }

  @State private var selectedTab: Tab = .home
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height
  @State private var showFeeds = false
  @State private var showEULA: Bool = !(
    UserDefaultsManager.shared.get(for: .eulaAccepted) as Bool? ?? false
  )
  @State private var showNotificationsNudge = false
  @State private var walletPhase: WalletPhase = .none
  @State private var walletOutcome: WalletPhase = .none
  @State private var showMandatoryAppUpdate = false
  @State private var showRecommendedAppUpdate = false

  @EnvironmentObject var eventBus: EventBus

  init(
    feedsViewController: FeedsViewController,
    leaderboardView: UINavigationController,
    uploadOptionsScreenView: UINavigationController,
    walletView: WalletView,
    profileView: ProfileView,
  ) {
    self.feedsViewController = feedsViewController
    self.uploadOptionsScreenView = uploadOptionsScreenView
    self.walletView = walletView
    self.profileView = profileView
    self.leaderboardView = leaderboardView
    let appearance = UITabBarAppearance()
    appearance.configureWithTransparentBackground()
    UITabBar.appearance().standardAppearance = appearance
    UITabBar.appearance().scrollEdgeAppearance = appearance
  }

  var body: some View {
    ZStack {
      TabView(selection: $selectedTab) {
        feedsViewControllerWrapper
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tag(Tab.home)

        ViewControllerWrapper(controller: leaderboardView)
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tag(Tab.leaderboard)

        ViewControllerWrapper(controller: uploadOptionsScreenView)
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tag(Tab.upload)

        walletView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tag(Tab.wallet)

        profileView
          .onUploadAction {
            suppressAnalytics = true
            selectedTab = .upload
          }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tag(Tab.profile)
      }
      .onChange(of: selectedTab) { tab in
        guard !suppressAnalytics else {
          suppressAnalytics = false
          return
        }
        tabDidChange(to: tab)
      }
      .onReceive(deepLinkRouter.$pendingDestination.compactMap { $0 }) { dest in
        switch dest {
        case .openVideo:
          selectedTab = .home
        case .home:
          selectedTab = .home
        case .leaderboard:
          selectedTab = .leaderboard
        case .addVideo:
          selectedTab = .upload
        case .aiGen:
          selectedTab = .upload
        case .wallet:
          selectedTab = .wallet
        case .profile:
          selectedTab = .profile
        case .profileAfterUpload:
          selectedTab = .profile
        }
        deepLinkRouter.pendingDestination = nil
      }
      .onReceive(eventBus.startPlayingTapped) {
        selectedTab = .home
      }
      .onReceive(eventBus.finishUploadingVideo) {
        UIView.setAnimationsEnabled(false)
        suppressAnalytics = true
        selectedTab = .home
        DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
          UIView.setAnimationsEnabled(false)
          UNUserNotificationCenter.current().getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .notDetermined, .denied:
              DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
                self.showNotificationsNudge = true
              }
            default: break
            }
          }
        }
      }
      .onReceive(eventBus.playGamesToEarnMoreTapped) {
        selectedTab = .home
      }
      .onReceive(eventBus.walletTapped) {
        selectedTab = .wallet
      }
      .hapticFeedback(.impact(weight: HapticFeedback.Weight.light), trigger: selectedTab)
      .fullScreenCover(isPresented: $showNotificationsNudge) {
        ZStack(alignment: .center) {
          NotificationsNudge {
            showNotificationsNudge = false
            AnalyticsModuleKt.getAnalyticsManager().trackEvent(event: PushNotificationsPopupEventData())
          }
          .background( ClearBackgroundView() )
        }
      }

      if walletPhase != .none {
        WalletOverlayView(
          phase: $walletPhase,
          outcome: $walletOutcome
        ) {
          withAnimation(.easeInOut(duration: CGFloat.animationPeriod)) {
            walletPhase = .none
          }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
        .background(Color.black.opacity(Constants.walletOpacity))
        .transition(.opacity)
      }
    }
    .safeAreaInset(edge: .bottom) {
      YralTabbar(
        selectedTab: $selectedTab,
        icons: [
          (Constants.homeIconImageNameSelected, Constants.homeIconImageNameUnselected),
          (Constants.leaderboardIconImageNameSelected, Constants.leaderboardIconImageNameUnselected),
          (Constants.uploadIconImageNameSelected, Constants.uploadIconImageNameUnselected),
          (Constants.walletIconImageNameSelected, Constants.walletIconImageNameUnselected),
          (Constants.profileIconImageNameSelected, Constants.profileIconImageNameUnSelected)
        ]
      )
    }
    .onAppear {
      let status = AppUpdateHandler.shared.getAppUpdateStatus()
      switch status {
      case .none:
        break
      case .force:
        self.showMandatoryAppUpdate = true
      case .recommended:
        self.showRecommendedAppUpdate = true
      }
    }
    .fullScreenCover(isPresented: $showEULA) {
      EULAPopupView(isPresented: $showEULA) {
        UserDefaultsManager.shared.set(true, for: .eulaAccepted)
        NotificationCenter.default.post(name: .eulaAcceptedChanged, object: nil)
      }
      .background( ClearBackgroundView() )
    }
    .fullScreenCover(isPresented: $showMandatoryAppUpdate) {
      MandatoryUpdateView()
    }
    .fullScreenCover(isPresented: $showRecommendedAppUpdate) {
      RecommendedUpdatePopUp {
        showRecommendedAppUpdate = false
      }
      .background( ClearBackgroundView() )
    }
    .ignoresSafeArea([.keyboard])
  }

  @ViewBuilder
  private func tabIcon(selected: Bool, selectedName: String, unselectedName: String) -> some View {
    Image(
      ImageResource(
        name: selected ? selectedName : unselectedName,
        bundle: .main
      )
    )
    .renderingMode(.original)
  }

  private func tabDidChange(to tab: Tab) {
    var categoryName = CategoryName.home
    switch tab {
    case .home:
      categoryName = .home
    case .leaderboard:
      categoryName = .leaderboard
    case .upload:
      categoryName = .uploadVideo
    case .wallet:
      categoryName = .home
    case .profile:
      categoryName = .profile
    }
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: BottomNavigationClickedEventData(
        categoryName: categoryName
      )
    )
  }
}

extension HomeTabController {
  enum Constants {
    static let homeIconImageNameUnselected = "home_tab_unselected"
    static let homeIconImageNameSelected = "home_tab_selected"
    static let leaderboardIconImageNameSelected = "leaderboard_tab_selected"
    static let leaderboardIconImageNameUnselected = "leaderboard_tab_unselected"
    static let walletIconImageNameSelected = "wallet_tab_selected"
    static let walletIconImageNameUnselected = "wallet_tab_unselected"
    static let uploadIconImageNameUnselected = "upload_tab_unselected"
    static let uploadIconImageNameSelected = "upload_tab_selected"
    static let profileIconImageNameUnSelected = "profile_tab_unselected"
    static let profileIconImageNameSelected = "profile_tab_selected"
    static let tabIndicatorHeight: CGFloat = 2.0
    static let indicatorWidth = 30.0
    static let indicatorColor = YralColor.primary300.swiftUIColor
    static let walletOpacity = 0.8
  }
}

enum Tab: Hashable {
  case home, leaderboard, wallet, upload, profile

  var intValue: Int {
    switch self {
    case .home:
      return .zero
    case .leaderboard:
      return .one
    case .upload:
      return .two
    case .wallet:
      return .three
    case .profile:
      return .four
    }
  }
}
