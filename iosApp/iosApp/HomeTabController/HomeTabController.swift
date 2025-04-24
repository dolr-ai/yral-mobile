import SwiftUI

struct HomeTabController: View {
  @EnvironmentObject var session: SessionManager
  let feedsViewController: FeedsViewController
  let accountView: AccountView
  let uploadView: UploadView
  let profileView: ProfileView

  private var feedsViewControllerWrapper: FeedsViewControllerWrapper {
    FeedsViewControllerWrapper(
      feedsViewController: feedsViewController,
      showFeeds: $showFeeds
    )
  }

  @State private var selectedTab: Int = .zero
  @State private var tabBarHeight = UITabBarController().tabBar.frame.height
  @State private var showFeeds = false
  @State private var showEULA = !KeychainHelper.bool(for: Constants.eulaAccepted, default: false)

  init(
    feedsViewController: FeedsViewController,
    uploadView: UploadView,
    profileView: ProfileView,
    accountView: AccountView
  ) {
    self.feedsViewController = feedsViewController
    self.uploadView = uploadView
    self.profileView = profileView
    self.accountView = accountView
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

        uploadView
          .onDoneAction { selectedTab = .zero }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .one,
                             selectedName: Constants.uploadIconImageNameSelected,
                             unselectedName: Constants.uploadIconImageNameUnselected) }
          .tag(Int.one)

        profileView
          .onUploadAction { selectedTab = .one }
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem { tabIcon(selected: selectedTab == .two,
                             selectedName: Constants.profileIconImageNameSelected,
                             unselectedName: Constants.profileIconImageNameUnSelected) }
          .tag(Int.two)

        accountView
          .background(Color.black.edgesIgnoringSafeArea(.all))
          .tabItem {
            Image(ImageResource(name: Constants.accountIconImageName, bundle: .main))
              .renderingMode(.original)
          }
          .tag(Int.three)
      }
      GeometryReader { geometry in
        let tabWidth = geometry.size.width / .four
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
        try? KeychainHelper.store(true, for: Constants.eulaAccepted)
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
    static let uploadIconImageNameUnselected = "upload_tab_unselected"
    static let uploadIconImageNameSelected = "upload_tab_selected"
    static let profileIconImageNameUnSelected = "profile_tab_unselected"
    static let profileIconImageNameSelected = "profile_tab_selected"
    static let accountIconImageName = "account_tab"
    static let tabIndicatorHeight: CGFloat = 2.0
    static let indicatorWidth = 30.0
    static let indicatorColor = YralColor.primary300.swiftUIColor
    static let eulaAccepted = "yral.eula.accepted"
  }
}
