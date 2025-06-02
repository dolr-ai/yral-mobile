import SwiftUI
import Firebase
import FirebaseMessaging

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    FirebaseApp.configure()
    Messaging.messaging().delegate = self
    UNUserNotificationCenter.current().delegate = self
    return true
  }

  func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
    guard let token = fcmToken else { return }
    NotificationCenter.default.post(name: .registrationTokenUpdated, object: token)
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    let userInfo = response.notification.request.content.userInfo
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    return [.banner, .sound, .badge]
  }

  func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    Messaging.messaging().apnsToken = deviceToken
  }
}

@main
struct IosApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
  private let appDIContainer: AppDIContainer
  @State private var feedsDIContainer: FeedDIContainer?
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
       let accountDIContainer = accountDIContainer,
       let uploadDIContainer = uploadDIContainer,
       let profileDIContainer = profileDIContainer {
      HomeTabController(
        feedsViewController: feedsDIContainer.makeFeedsViewController(),
        uploadView: uploadDIContainer.makeUploadView(),
        profileView: profileDIContainer.makeProfileView(),
        accountView: accountDIContainer.makeAccountView()
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
      try await appDIContainer.authClient.initialize()
      feedsDIContainer = await appDIContainer.makeFeedDIContainer()
      uploadDIContainer = appDIContainer.makeUploadDIContainer()
      profileDIContainer = appDIContainer.makeProfileDIContainer()
      accountDIContainer = appDIContainer.makeAccountDIContainer()
    } catch {
      initializationError = error
    }
  }
}
