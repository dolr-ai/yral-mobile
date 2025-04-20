import SwiftUI
import Firebase

class AppDelegate: NSObject, UIApplicationDelegate {
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    FirebaseApp.configure()
    return true
  }
}

@main
struct IosApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
  let appDIContainer = AppDIContainer()
  @State private var feedsDIContainer: FeedDIContainer?
  @State private var profileDIContainer: ProfileDIContainer?
  @State private var uploadDIContainer: UploadDIContainer?
  @State private var accountDIContainer: AccountDIContainer?
  @State private var initializationError: Error?

  var body: some Scene {
    WindowGroup {
      if let feedsDIContainer = feedsDIContainer,
         let accountDIContainer = accountDIContainer,
         let uploadDIContainer = uploadDIContainer,
         let profileDIContainer = profileDIContainer {
        HomeTabController(
          feedsViewController: feedsDIContainer.makeFeedsViewController(),
          uploadView: uploadDIContainer.makeUploadView(),
          profileView: profileDIContainer.makeProfileView(),
          accountView: accountDIContainer.makeAccountView()
        )
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
  }

  @MainActor
  private func initializeDependencies() async {
    do {
      try await appDIContainer.authClient.initialize()
      feedsDIContainer = appDIContainer.makeFeedDIContainer()
      uploadDIContainer = appDIContainer.makeUploadDIContainer()
      profileDIContainer = appDIContainer.makeProfileDIContainer()
      accountDIContainer = appDIContainer.makeAccountDIContainer()
    } catch {
      initializationError = error
    }
  }
}
