import SwiftUI

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

  @State private var selectedTab: Int = .zero
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height
  @State private var showFeeds = false
  @State private var showEULA: Bool = !(UserDefaultsManager.shared.get(for: .eulaAccepted) as Bool? ?? false)

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
          .tabItem { tabIcon(selected: selectedTab == .zero,
                             selectedName: Constants.homeIconImageNameSelected,
                             unselectedName: Constants.homeIconImageNameUnselected) }
          .tag(Int.zero)

        leaderboardView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            tabIcon(selected: selectedTab == .one,
                    selectedName: Constants.leaderboardIconImageNameSelected,
                    unselectedName: Constants.leaderboardIconImageNameUnselected)
          }
          .tag(Int.one)

        uploadView
          .onDoneAction { selectedTab = .zero }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .two,
                             selectedName: Constants.uploadIconImageNameSelected,
                             unselectedName: Constants.uploadIconImageNameUnselected) }
          .tag(Int.two)

        profileView
          .onUploadAction { selectedTab = .one }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .three,
                             selectedName: Constants.profileIconImageNameSelected,
                             unselectedName: Constants.profileIconImageNameUnSelected) }
          .tag(Int.three)

        accountView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            Image(ImageResource(name: Constants.accountIconImageName, bundle: .main))
              .renderingMode(.original)
          }
          .tag(Int.four)
      }
      GeometryReader { geometry in
        let tabWidth = geometry.size.width / .five
        let indicatorXPosition = CGFloat(selectedTab) * tabWidth + (tabWidth - Constants.indicatorWidth) / .two
        VStack {
          Spacer().frame(height: geometry.size.height)
          HStack {
            Spacer().frame(width: indicatorXPosition)
            Rectangle()
              .fill(Constants.indicatorColor)
              .frame(width: Constants.indicatorWidth, height: Constants.tabIndicatorHeight)
              .cornerRadius(Constants.tabIndicatorHeight / .two)
              .offset(x: .zero, y: -self.tabBarHeight)
              .animation(.easeInOut, value: selectedTab)
            Spacer()
          }
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
