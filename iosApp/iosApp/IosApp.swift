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
<<<<<<< HEAD
  @State private var profileDIContainer: ProfileDIContainer?
  @State private var initializationError: Error?

  var body: some Scene {
    WindowGroup {
      if let feedsDIContainer = feedsDIContainer, let profileDIContainer = profileDIContainer {
        HomeTabController(
          feedsViewControllerWrapper: feedsDIContainer.makeFeedsViewControllerWrapper(),
          profileView: profileDIContainer.makeProfileView()
        )
=======
  @State private var initializationError: Error?
  var body: some Scene {
    WindowGroup {
      if let feedsDIContainer = feedsDIContainer {
        feedsDIContainer.makeFeedsViewControllerWrapper()
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
      } else if let error = initializationError {
        Text("Failed to initialize: \(error.localizedDescription)")
          .foregroundColor(.red)
      } else {
<<<<<<< HEAD
        SplashScreenView()
=======
        ProgressView("Initializing...")
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
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
<<<<<<< HEAD
      profileDIContainer = appDIContainer.makeProfileDIContainer()
=======
>>>>>>> 6c3bf61 (Stiches the feeds flow and adds YralPlayer (#74))
    } catch {
      initializationError = error
    }
  }
}
