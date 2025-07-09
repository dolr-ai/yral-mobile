import SwiftUI
import iosSharedUmbrella

struct HomeTabController: View {
  @EnvironmentObject var session: SessionManager
  let feedsViewController: FeedsViewController
  let accountView: AccountView
  let uploadView: UploadView
  let profileView: ProfileView
  let leaderboardView: LeaderboardView

  private var feedsViewControllerWrapper: FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(
      feedsViewController: feedsViewController,
      showFeeds: $showFeeds
    )
  }

  @State private var selectedTab: Tab = .home
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height
  @State private var showFeeds = false
  @State private var showEULA: Bool = !(
    UserDefaultsManager.shared.get(for: .eulaAccepted) as Bool? ?? false
  )
  @State private var showNotificationsNudge = false

  init(
    feedsViewController: FeedsViewController,
    uploadView: UploadView,
    profileView: ProfileView,
    accountView: AccountView,
    leaderboardView: LeaderboardView
  ) {
    self.feedsViewController = feedsViewController
    self.uploadView = uploadView
    self.profileView = profileView
    self.accountView = accountView
    self.leaderboardView = leaderboardView
    UITabBar.appearance().backgroundColor = .black
    UITabBar.appearance().barTintColor = .black
    UITabBar.appearance().isTranslucent = false
  }

  var body: some View {
    ZStack {
      TabView(selection: $selectedTab) {
        feedsViewControllerWrapper
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .home,
                             selectedName: Constants.homeIconImageNameSelected,
                             unselectedName: Constants.homeIconImageNameUnselected) }
          .tag(Tab.home)

        leaderboardView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            tabIcon(selected: selectedTab == .leaderboard,
                    selectedName: Constants.leaderboardIconImageNameSelected,
                    unselectedName: Constants.leaderboardIconImageNameUnselected)
          }
          .tag(Tab.leaderboard)

        uploadView
          .onDoneAction {
            UIView.setAnimationsEnabled(false)
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
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .upload,
                             selectedName: Constants.uploadIconImageNameSelected,
                             unselectedName: Constants.uploadIconImageNameUnselected) }
          .tag(Tab.upload)

        profileView
          .onUploadAction { selectedTab = .upload }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .profile,
                             selectedName: Constants.profileIconImageNameSelected,
                             unselectedName: Constants.profileIconImageNameUnSelected) }
          .tag(Tab.profile)

        accountView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            Image(
              ImageResource(name: Constants.accountIconImageName, bundle: .main)
            )
            .renderingMode(.original)
          }
          .tag(Tab.account)
      }
      .onChange(of: selectedTab) { tab in
        tabDidChange(to: tab)
      }
      .onReceive(
        NotificationCenter.default.publisher(
          for: .videoUploadNotificationReceived
        )
      ) { _ in
        uploadNotificationReceived()
      }
      .fullScreenCover(isPresented: $showNotificationsNudge) {
        ZStack(alignment: .center) {
          NotificationsNudge {
            showNotificationsNudge = false
          }
          .background( ClearBackgroundView() )
        }
      }
      GeometryReader { geometry in
        let tabWidth = geometry.size.width / .five
        let indicatorXPosition = CGFloat(selectedTab.intValue) * tabWidth + (tabWidth - Constants.indicatorWidth) / .two
        VStack {
          Spacer().frame(height: geometry.size.height)
          HStack {
            Spacer().frame(width: indicatorXPosition)
            Rectangle()
              .fill(Constants.indicatorColor)
              .frame(
                width: Constants.indicatorWidth,
                height: Constants.tabIndicatorHeight
              )
              .cornerRadius(Constants.tabIndicatorHeight / .two)
              .offset(x: .zero, y: -self.tabBarHeight)
              .animation(.easeInOut, value: selectedTab)
            Spacer()
          }

          //          Text("New")
          //            .font(YralFont.pt8.bold.swiftUIFont)
          //            .foregroundColor(YralColor.grey0.swiftUIColor)
          //            .padding(.horizontal, 4)
          //            .background(YralColor.primary300.swiftUIColor)
          //            .clipShape(RoundedRectangle(cornerRadius: 12))
          //            .offset(x: -tabWidth, y: -self.tabBarHeight - 2.5)
        }
      }
    }
    .fullScreenCover(isPresented: $showEULA) {
      EULAPopupView(isPresented: $showEULA) {
        UserDefaultsManager.shared.set(true, for: .eulaAccepted)
        NotificationCenter.default.post(name: .eulaAcceptedChanged, object: nil)
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
    case .profile:
      categoryName = .profile
    case .account:
      categoryName = .menu
    }
    AnalyticsModuleKt.getAnalyticsManager().trackEvent(
      event: BottomNavigationClickedEventData(
        categoryName: categoryName
      )
    )
  }

  @MainActor func uploadNotificationReceived() {
    DispatchQueue.main.asyncAfter(deadline: .now() + CGFloat.animationPeriod) {
      self.selectedTab = .profile
      Task {
        try? await Task.sleep(nanoseconds: 300_000_000)
        await profileView.refreshVideosFromPushNotifications()
      }
    }
  }
}

extension HomeTabController {
  enum Constants {
    static let homeIconImageNameUnselected = "home_tab_unselected"
    static let homeIconImageNameSelected = "home_tab_selected"
    static let leaderboardIconImageNameSelected = "leaderboard_tab_selected"
    static let leaderboardIconImageNameUnselected = "leaderboard_tab_unselected"
    static let uploadIconImageNameUnselected = "upload_tab_unselected"
    static let uploadIconImageNameSelected = "upload_tab_selected"
    static let profileIconImageNameUnSelected = "profile_tab_unselected"
    static let profileIconImageNameSelected = "profile_tab_selected"
    static let accountIconImageName = "account_tab"
    static let tabIndicatorHeight: CGFloat = 2.0
    static let indicatorWidth = 30.0
    static let indicatorColor = YralColor.primary300.swiftUIColor
  }
}

enum Tab: Hashable {
  case home, leaderboard, upload, profile, account

  var intValue: Int {
    switch self {
    case .home:
      return .zero
    case .leaderboard:
      return .one
    case .upload:
      return .two
    case .profile:
      return .three
    case .account:
      return .four
    }
  }
}
