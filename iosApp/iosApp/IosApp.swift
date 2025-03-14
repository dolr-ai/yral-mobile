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
  @State private var accountDIContainer: AccountDIContainer?
  @State private var uploadDIContainer: UploadDIContainer?
  @State private var initializationError: Error?

  var body: some Scene {
    WindowGroup {
      if let feedsDIContainer = feedsDIContainer,
         let accountDIContainer = accountDIContainer,
         let uploadDIContainer = uploadDIContainer {
        HomeTabController(
          feedsViewControllerWrapper: feedsDIContainer.makeFeedsViewControllerWrapper(),
          accountView: accountDIContainer.makeAccountView(),
          uploadView: uploadDIContainer.makeUploadView()
        )
      } else if let error = initializationError {
        Text("Failed to initialize: \(error.localizedDescription)")
          .foregroundColor(.red)
      } else {
        SplashScreenView()
          .task {
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
      accountDIContainer = appDIContainer.makeAccountDIContainer()
      uploadDIContainer = appDIContainer.makeUploadDIContainer()
    } catch {
      initializationError = error
    }
  }
}
