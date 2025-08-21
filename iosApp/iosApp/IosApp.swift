import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import iosSharedUmbrella
import FBSDKCoreKit
import FirebaseMessaging
import Mixpanel
import MixpanelSessionReplay
import BranchSDK

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    if UIApplication.shared.isProtectedDataAvailable {
      migrateKeychain()
    }

    Branch.getInstance().initSession(launchOptions: launchOptions) { (params, error) in
      if let params {
        print("\(Constants.branchParameters) \(params)")
      } else if let error {
        print("\(Constants.branchError) \(error.localizedDescription)")
      }
    }

    NotificationCenter.default.addObserver(
      forName: UIApplication.protectedDataDidBecomeAvailableNotification,
      object: nil,
      queue: .main
    ) { [weak self] _ in
      guard let self = self else { return }
      self.migrateKeychain()
    }

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
    _ application: UIApplication,
    continue userActivity: NSUserActivity,
    restorationHandler: @escaping ([any UIUserActivityRestoring]?) -> Void
  ) -> Bool {
    Branch.getInstance().continue(userActivity)
    return true
  }

  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    Branch.getInstance().application(app, open: url, options: options)
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

  private func migrateKeychain() {
    typealias AuthConstants = DefaultAuthClient.Constants
    let keychainMigrationDone: Bool? = UserDefaultsManager.shared.get(for: .keychainMigrationDone) as Bool? ?? false
    guard !(keychainMigrationDone ?? true) else { return }

    let keys = [
      AuthConstants.keychainIdentity,
      AuthConstants.keychainUserPrincipal,
      AuthConstants.keychainCanisterPrincipal,
      AuthConstants.keychainAccessToken,
      AuthConstants.keychainIDToken,
      AuthConstants.keychainRefreshToken
    ]

    for key in keys {
      if let data = try? KeychainHelper.retrieveData(for: key) {
        try? KeychainHelper.store(data: data, for: key)
      }
    }

    UserDefaultsManager.shared.set(true, for: .keychainMigrationDone)
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
  @State private var uploadOptionsDIContainer: UploadOptionsDIContainer?
  @State private var accountDIContainer: AccountDIContainer?
  @State private var initializationError: Error?
  @StateObject private var session: SessionManager
  @StateObject private var deepLinkRouter = DeepLinkRouter.shared
  @StateObject private var eventBus = EventBus()
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
        .environmentObject(eventBus)
        .environment(\.appDIContainer, appDIContainer)
    }
  }

  @ViewBuilder private func contentView() -> some View {
    if let feedsDIContainer = feedsDIContainer,
       let leaderboardDIContainer = leaderboardDIContainer,
       let uploadOptionsDIContainer = uploadOptionsDIContainer,
       let accountDIContainer = accountDIContainer,
       let profileDIContainer = profileDIContainer {
      HomeTabController(
        feedsViewController: feedsDIContainer.makeFeedsViewController(playToScroll: false),
        uploadOptionsScreenView: uploadOptionsDIContainer.makeUploadOptionsView(),
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
      uploadOptionsDIContainer = appDIContainer.makeUploadOptionsDIContainer()
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
    static let branchParameters = "Branch parameters:"
    static let branchError = "Branch error:"
  }
}
