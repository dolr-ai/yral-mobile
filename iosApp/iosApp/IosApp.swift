import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import iosSharedUmbrella
import FBSDKCoreKit
import FirebaseMessaging
import Mixpanel
import MixpanelSessionReplay

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
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
    Messaging.messaging().delegate = self
    UNUserNotificationCenter.current().delegate = self
    return true
  }

  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    return ApplicationDelegate.shared.application(app, open: url, options: options)
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
    if isUploadNotification(userInfo: response.notification.request.content.userInfo) {
      DeepLinkRouter.shared.pendingDestination = DeepLinkRouter.Destination.profileAfterUpload
    }
    completionHandler()
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    if isUploadNotification(userInfo: notification.request.content.userInfo) {
      ToastManager.showToast(type: .uploadSuccess) { }
      onTap: {
        DeepLinkRouter.shared.pendingDestination = .profileAfterUpload
      }
    }
    return []
  }

  func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    Messaging.messaging().apnsToken = deviceToken
  }

  func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: any Error) {
    print(error)
  }

  private func isUploadNotification(userInfo: [AnyHashable: Any]) -> Bool {
    if let payloadString = userInfo[Constants.payloadString] as? String,
       let data = payloadString.data(using: .utf8),
       let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
       let type = json[Constants.typeString] as? String {
      if type == Constants.videoUploadSuccessType {
        return true
      }
    }
    return false
  }
}

@main
struct IosApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
  private let appDIContainer: AppDIContainer
  private let mixpanelToken = "MIXPANEL_TOKEN"
  @State private var feedsDIContainer: FeedDIContainer?
  @State private var leaderboardDIContainer: LeaderboardDIContainer?
  @State private var profileDIContainer: ProfileDIContainer?
  @State private var uploadDIContainer: UploadDIContainer?
  @State private var accountDIContainer: AccountDIContainer?
  @State private var initializationError: Error?
  @StateObject private var session: SessionManager
  @StateObject private var deepLinkRouter = DeepLinkRouter.shared
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
        .environmentObject(deepLinkRouter)
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
      .environmentObject(deepLinkRouter)
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
      AnalyticsModuleKt.getAnalyticsManager().initialise()
      if let mixpanelToken = Bundle.main.infoDictionary?[mixpanelToken] as? String {
        let config = MPSessionReplayConfig(wifiOnly: false, autoMaskedViews: [.web])
        MPSessionReplay.initialize(
          token: mixpanelToken,
          distinctId: Mixpanel.sharedInstance()?.distinctId ?? "",
          config: config
        )
      }
      AnalyticsModuleKt.getAnalyticsManager().trackEvent(event: SplashScreenViewedEventData())
      feedsDIContainer = await appDIContainer.makeFeedDIContainer()
      try await appDIContainer.authClient.initialize()
      uploadDIContainer = appDIContainer.makeUploadDIContainer()
      profileDIContainer = appDIContainer.makeProfileDIContainer()
      accountDIContainer = appDIContainer.makeAccountDIContainer()
      leaderboardDIContainer = appDIContainer.makeLeaderboardDIContainer()
    } catch {
      initializationError = error
    }
  }
}

extension AppDelegate {
  public enum Constants {
    static let payloadString = "payload"
    static let typeString = "type"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
  }
}
