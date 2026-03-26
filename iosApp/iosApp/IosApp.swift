import SwiftUI
import FirebaseCore
import FirebaseAppCheck
import FirebaseMessaging
import FBSDKCoreKit
import BranchSDK
import UserNotifications
import iosSharedUmbrella

private enum AppEnvironment {
  static let xctestConfigurationFilePath = "XCTestConfigurationFilePath"

  static var isRunningTests: Bool {
    ProcessInfo.processInfo.environment[xctestConfigurationFilePath] != nil
  }
}

private enum AppStorageKeys {
  static let keychainMigrationDone = "keychainMigrationDone"
}

private enum LegacyAuthKeychainKeys {
  // Preserve previously stored auth material before the local auth layer is removed.
  static let identity = "yral.delegatedIdentity"
  static let userPrincipal = "yral.userPrincipal"
  static let canisterPrincipal = "yral.canisterPrincipal"
  static let accessToken = "yral.accessToken"
  static let idToken = "yral.idToken"
  static let refreshToken = "yral.refreshToken"
}

private final class EmptyExternalDependencyProvider: ExternalDependencyProvider {
}

enum AppRouteResolver {
  enum Constants {
    static let payloadString = "payload"
    static let typeString = "type"
    static let internalURL = "internalUrl"
    static let deeplinkPath = "$deeplink_path"
    static let branchClick = "+clicked_branch_link"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
  }

  static func notificationRoute(
    from userInfo: [AnyHashable: Any],
    parseUrl: (String) -> AppRoute = defaultParseUrl
  ) -> AppRoute? {
    guard let payloadString = userInfo[Constants.payloadString] as? String,
          let payloadData = payloadString.data(using: .utf8),
          let rawPayload = try? JSONSerialization.jsonObject(with: payloadData),
          let payload = rawPayload as? [String: Any]
    else {
      return nil
    }

    if let internalURL = payload[Constants.internalURL] as? String {
      return parseUrl(internalURL)
    }

    if let type = payload[Constants.typeString] as? String,
       type == Constants.videoUploadSuccessType {
      return VideoUploadSuccessful(videoID: nil)
    }

    return nil
  }

  static func branchRoute(
    from params: [AnyHashable: Any],
    parseUrl: (String) -> AppRoute = defaultParseUrl
  ) -> AppRoute? {
    guard isBranchClick(params[Constants.branchClick]) else {
      return nil
    }

    if let type = params[Constants.typeString] as? String,
       type == Constants.videoUploadSuccessType {
      return VideoUploadSuccessful(videoID: nil)
    }

    if let deeplinkPath = params[Constants.deeplinkPath] as? String {
      return parseUrl(deeplinkPath)
    }

    return nil
  }

  static func isBranchClick(_ value: Any?) -> Bool {
    switch value {
    case let bool as Bool:
      return bool
    case let number as NSNumber:
      return number.intValue == 1
    case let string as String:
      let lowered = string.lowercased()
      return lowered == "1" || lowered == "true"
    default:
      return false
    }
  }

  private static func defaultParseUrl(_ url: String) -> AppRoute {
    AppDIHelper().getRoutingService().parseUrl(url: url)
  }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
  lazy var root: RootComponent = DefaultRootComponent(
    componentContext: DefaultComponentContext(lifecycle: ApplicationLifecycle())
  )

  private var saveUtmParams: Task<Void, Never>?

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    guard !AppEnvironment.isRunningTests else {
      return true
    }

    if UIApplication.shared.isProtectedDataAvailable {
      migrateKeychainIfNeeded()
    }

#if DEBUG
    Branch.setUseTestBranchKey(true)
#endif

    Branch.getInstance().initSession(launchOptions: launchOptions) { [weak self] params, _ in
      guard let self,
            let params
      else {
        return
      }

      self.handleAffiliateAttribution(params: params)
      self.navigate(to: AppRouteResolver.branchRoute(from: params))
    }

    NotificationCenter.default.addObserver(
      forName: UIApplication.protectedDataDidBecomeAvailableNotification,
      object: nil,
      queue: .main
    ) { [weak self] _ in
      self?.migrateKeychainIfNeeded()
    }

#if DEBUG
    AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
#else
    AppCheck.setAppCheckProviderFactory(YralAppCheckProviderFactory())
#endif

    FirebaseApp.configure()
    _ = root
    _ = ApplicationDelegate.shared.application(
      application,
      didFinishLaunchingWithOptions: launchOptions
    )
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
    let branchHandled = Branch.getInstance().application(app, open: url, options: options)
    let facebookHandled = ApplicationDelegate.shared.application(app, open: url, options: options)
    return branchHandled || facebookHandled
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    navigate(to: AppRouteResolver.notificationRoute(from: response.notification.request.content.userInfo))
    completionHandler()
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    []
  }

  func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    Messaging.messaging().apnsToken = deviceToken
  }

  private func navigate(to route: AppRoute?) {
    guard let route else {
      return
    }

    Task { @MainActor in
      root.onNavigationRequest(appRoute: route)
    }
  }

  private func handleAffiliateAttribution(params: [AnyHashable: Any]) {
    guard AppRouteResolver.isBranchClick(params["+clicked_branch_link"]) else {
      return
    }

    guard let channel = (
      params["~channel"] as? String
      ?? params["channel"] as? String
    )?.trimmingCharacters(in: .whitespacesAndNewlines),
          !channel.isEmpty
    else {
      return
    }

    AppDIHelper().getAffiliateAttributionStore().storeIfEmpty(affiliate: channel)
    saveUtmParams?.cancel()
    saveUtmParams = Task {
      do {
        try await AppDIHelper()
          .getUtmAttributionStore()
          .storeIfEmpty(
            source: params["utm_source"] as? String,
            medium: params["utm_medium"] as? String,
            campaign: params["utm_campaign"] as? String,
            term: params["utm_term"] as? String,
            content: params["utm_content"] as? String,
            raw: params["raw_referrer"] as? String
          )
      } catch {
      }
    }
  }

  private func migrateKeychainIfNeeded() {
    guard !UserDefaults.standard.bool(forKey: AppStorageKeys.keychainMigrationDone) else {
      return
    }

    let keys = [
      LegacyAuthKeychainKeys.identity,
      LegacyAuthKeychainKeys.userPrincipal,
      LegacyAuthKeychainKeys.canisterPrincipal,
      LegacyAuthKeychainKeys.accessToken,
      LegacyAuthKeychainKeys.idToken,
      LegacyAuthKeychainKeys.refreshToken
    ]

    for key in keys {
      if let data = try? KeychainHelper.retrieveData(for: key) {
        try? KeychainHelper.store(data: data, for: key)
      }
    }

    UserDefaults.standard.set(true, forKey: AppStorageKeys.keychainMigrationDone)
  }

  deinit {
    saveUtmParams?.cancel()
  }
}

@main
struct IosApp: App {
  @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

  init() {
    guard !AppEnvironment.isRunningTests else {
      return
    }

    AppDIKt.doInitKoin { coreApplication in
      coreApplication.installExternalDependencyModule(provider: EmptyExternalDependencyProvider())
    }
    AppSetupKt.initializeApp()
  }

  var body: some Scene {
    WindowGroup {
      if AppEnvironment.isRunningTests {
        EmptyView()
      } else {
        RootView(root: delegate.root)
          .ignoresSafeArea(edges: .all)
          .ignoresSafeArea(.keyboard)
          .edgesIgnoringSafeArea(.all)
          .onOpenURL { url in
            _ = Branch.getInstance().application(UIApplication.shared, open: url, options: [:])
          }
          .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
            Branch.getInstance().continue(activity)
          }
      }
    }
  }
}
