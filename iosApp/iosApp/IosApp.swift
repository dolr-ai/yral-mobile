import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import iosSharedUmbrella
import FBSDKCoreKit

class AppDelegate: NSObject, UIApplicationDelegate {
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
#if DEBUG
    AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
#else
    let providerFactory = YralAppCheckProviderFactory()
    AppCheck.setAppCheckProviderFactory(providerFactory)
#endif

    FirebaseApp.configure()
    ApplicationDelegate.shared.application(
      application,
      didFinishLaunchingWithOptions: launchOptions
    )

#if DEBUG
    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
      AppCheck.appCheck().token(forcingRefresh: true) { token, error in
        if let error = error {
          print("Sarvesh 1 error: \(error.localizedDescription)")
        } else {
          print("Sarvesh 1 Token: \(token?.token ?? "nil")")
        }
      }
    }
    Task {
      do {
        let token = try await AppCheck.appCheck().limitedUseToken()
        print("Sarvesh token: \(token)")
      } catch {
        print("Sarvesh error: \(error)")
      }
    }
#endif
    return true
  }

  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    return ApplicationDelegate.shared.application(app, open: url, options: options)
  }
}

@main
struct IosApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
  private let appDIContainer: AppDIContainer
  @State private var feedsDIContainer: FeedDIContainer?
  @State private var leaderboardDIContainer: LeaderboardDIContainer?
  @State private var profileDIContainer: ProfileDIContainer?
  @State private var uploadDIContainer: UploadDIContainer?
  @State private var accountDIContainer: AccountDIContainer?
  @State private var initializationError: Error?
  @StateObject private var session: SessionManager
  @State private var authStatus: AuthState = .uninitialized

  init() {
    let container = AppDIContainer()
    appDIContainer = container
    _session = StateObject(
      wrappedValue: container.session
    )
  }

  var body: some Scene {
    WindowGroup {
      contentView()
    }
  }

  @ViewBuilder private func contentView() -> some View {
    if let feedsDIContainer = feedsDIContainer,
       let leaderboardDIContainer = leaderboardDIContainer,
       let accountDIContainer = accountDIContainer,
       let uploadDIContainer = uploadDIContainer,
       let profileDIContainer = profileDIContainer {
      HomeTabController(
        feedsViewController: feedsDIContainer.makeFeedsViewController(),
        uploadView: uploadDIContainer.makeUploadView(),
        profileView: profileDIContainer.makeProfileView(),
        accountView: accountDIContainer.makeAccountView(),
        leaderboardView: leaderboardDIContainer.makeLeaderboardView()
      )
      .environmentObject(session)
    } else if let error = initializationError {
      Text("Failed to initialize: \(error.localizedDescription)")
        .foregroundColor(.red)
    } else {
      SplashScreenView()
        .task {
          await HLSDownloadManager().removeAllBookmarkedAssetsOnLaunch()
          await initializeDependencies()
        }
    }
  }

  @MainActor
  private func initializeDependencies() async {
    do {
      AppDI_iosKt.doInitKoin { _ in  }
      try await appDIContainer.authClient.initialize()
      feedsDIContainer = await appDIContainer.makeFeedDIContainer()
      uploadDIContainer = appDIContainer.makeUploadDIContainer()
      profileDIContainer = appDIContainer.makeProfileDIContainer()
      accountDIContainer = appDIContainer.makeAccountDIContainer()
      leaderboardDIContainer = appDIContainer.makeLeaderboardDIContainer()
    } catch {
      initializationError = error
    }
  }
}
