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
  let root: RootComponent = DefaultRootComponent(
      componentContext: DefaultComponentContext(lifecycle: ApplicationLifecycle())
  )
  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    if UIApplication.shared.isProtectedDataAvailable {
      migrateKeychain()
    }
    #if DEBUG
    Branch.setUseTestBranchKey(true)
    #endif

    Branch.getInstance().initSession(launchOptions: launchOptions) { [weak self] (params, error) in
      if let error = error {
        print("\(DeepLinkRouter.Constants.branchError)", error)
        return
      }

      guard let self = self else { return }

      if let dict = params as? [String: Any] {
        self.handleAffiliateAttribution(params: dict)
        Task { @MainActor in
          DeepLinkRouter.shared.resolve(from: dict)
        }
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
    Task { @MainActor in
      DeepLinkRouter.shared.resolve(from: response.notification.request.content.userInfo)
    }
    completionHandler()
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
  ) async -> UNNotificationPresentationOptions {
    if let payloadString = notification.request.content.userInfo[Constants.payloadString] as? String,
       let payloadData = payloadString.data(using: .utf8),
       let payloadDict = try? JSONSerialization.jsonObject(with: payloadData) as? [String: Any],
       let type = payloadDict[Constants.typeString] as? String,
       let internalURL = payloadDict[Constants.internalURL] as? String {
      if type == Constants.videoUploadSuccessType {
        ToastManager.showToast(type: .uploadSuccess) {}
        onTap: {
          DeepLinkRouter.shared.setRoute(route: VideoUploadSuccessful(videoID: nil))
        }
      } else if type == Constants.videoViewedRewardType {
        if let rewards = AppDIHelper().getRoutingService().parseUrl(url: internalURL) as? RewardsReceived {
          DeepLinkRouter.shared.setRoute(route: rewards)
        }
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

  private var saveUtmParams: Task<Void, Never>?

  private func handleAffiliateAttribution(params: [String: Any]) {
    guard isBranchClick(params["+clicked_branch_link"]) else { return }
    guard let channel = (
      params["~channel"] as? String
        ?? params["channel"] as? String
    )?.trimmingCharacters(in: .whitespacesAndNewlines),
      !channel.isEmpty
    else { return }

    AppDIHelper().getAffiliateAttributionStore().storeIfEmpty(affiliate: channel)
    saveUtmParams?.cancel()
    saveUtmParams = Task {
        do {
            // Capture UTM parameters (if present) for attribution
            let utmSource = params[Constants.utmSource] as? String
            let utmMedium = params[Constants.utmMedium] as? String
            let utmCampaign = params[Constants.utmCampaign] as? String
            let utmTerm = params[Constants.utmTerm] as? String
            let utmContent = params[Constants.utmContent] as? String

            try await AppDIHelper()
                .getUtmAttributionStore()
                .storeIfEmpty(
                    source: utmSource,
                    medium: utmMedium,
                    campaign: utmCampaign,
                    term: utmTerm,
                    content: utmContent
                )
        } catch {
            debugPrint("Cancelled or Failed")
        }
    }
  }

  deinit {
    saveUtmParams?.cancel()
  }

  private func isBranchClick(_ value: Any?) -> Bool {
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
  @State private var uploadOptionsDIContainer: UploadOptionsDIContainer?
  @State private var walletDIContainer: WalletDIContainer?
  @State private var profileDIContainer: ProfileDIContainer?
  @State private var initializationError: Error?
  @StateObject private var session: SessionManager
  @StateObject private var deepLinkRouter = DeepLinkRouter.shared
  @StateObject private var eventBus: EventBus
  @State private var authStatus: AuthState = .uninitialized
  @State private var showEULA: Bool = !(
    UserDefaultsManager.shared.get(for: .eulaAccepted) as Bool? ?? false
  )
  @State private var showRecommendedAppUpdate = false
  @State private var showMandatoryAppUpdate = false

  init() {
    let container = AppDIContainer()
    appDIContainer = container
    _session = StateObject(
      wrappedValue: container.session
    )
    _eventBus = StateObject(
      wrappedValue: container.eventBus
    )
    AppDIKt.doInitKoin { coreApplication in
      coreApplication.installExternalDependencyModule(provider: IosDependencyProvider())
    }
    AppSetupKt.initializeApp()
  }

  var body: some Scene {
    WindowGroup {
       RootView(root: delegate.root)
           .ignoresSafeArea(edges: .all)
           .ignoresSafeArea(.keyboard)
           .edgesIgnoringSafeArea(.all)
         .environmentObject(deepLinkRouter)
         .onReceive(deepLinkRouter.$appRoute.compactMap { $0 }) { route in
           delegate.root.onNavigationRequest(appRoute: route)
           deepLinkRouter.clearResolution()
         }
         .onOpenURL { url in
           Branch.getInstance().application(UIApplication.shared, open: url, options: [:])
         }
         .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
           Branch.getInstance().continue(activity)
         }
       .fullScreenCover(isPresented: $showEULA) {
             EULAPopupView(isPresented: $showEULA) {
               UserDefaultsManager.shared.set(true, for: .eulaAccepted)
             }
             .background( ClearBackgroundView() )
           }
           .fullScreenCover(isPresented: $showRecommendedAppUpdate) {
             RecommendedUpdatePopUp {
               showRecommendedAppUpdate = false
             }
             .background( ClearBackgroundView() )
           }
           .fullScreenCover(isPresented: $showMandatoryAppUpdate) {
             MandatoryUpdateView()
           }
           .onAppear {
             let status = AppUpdateHandler.shared.getAppUpdateStatus()
             switch status {
             case .none:
               break
             case .force:
               self.showMandatoryAppUpdate = true
             case .recommended:
               self.showRecommendedAppUpdate = true
             }
           }

//     contentView()
//       .environmentObject(deepLinkRouter)
//       .environmentObject(eventBus)
//       .environment(\.appDIContainer, appDIContainer)
//       .onOpenURL { url in
//         Branch.getInstance().application(UIApplication.shared, open: url, options: [:])
//       }
//       .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
//         Branch.getInstance().continue(activity)
//       }
    }
  }

  @ViewBuilder private func contentView() -> some View {
    if let feedsDIContainer = feedsDIContainer,
       let leaderboardDIContainer = leaderboardDIContainer,
       let uploadOptionsDIContainer = uploadOptionsDIContainer,
       let walletDIContainer = walletDIContainer,
       let profileDIContainer = profileDIContainer {
      let flagManager = AppDIHelper().getFeatureFlagManager()
      HomeTabController(
        feedsViewController: feedsDIContainer.makeFeedsViewController(
          playToScroll: flagManager.isEnabled(flag: FeedFeatureFlags.SmileyGame.shared.StopAndVoteNudge)
        ),
        leaderboardView: leaderboardDIContainer.makeLeaderboardView(),
        uploadOptionsScreenView: uploadOptionsDIContainer.makeUploadOptionsView(),
        walletView: walletDIContainer.makeWalletView(),
        profileView: profileDIContainer.makeProfileView()
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
      leaderboardDIContainer = appDIContainer.makeLeaderboardDIContainer()
      uploadOptionsDIContainer = appDIContainer.makeUploadOptionsDIContainer()
      walletDIContainer = appDIContainer.makeWalletDIContainer()
      profileDIContainer = appDIContainer.makeProfileDIContainer()
    } catch {
      initializationError = error
    }
  }
}

extension AppDelegate {
  public enum Constants {
    static let payloadString = "payload"
    static let typeString = "type"
    static let internalURL = "internalUrl"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
    static let videoViewedRewardType = "RewardEarned"
    static let utmSource = "utm_source"
    static let utmMedium = "utm_medium"
    static let utmCampaign = "utm_campaign"
    static let utmTerm = "utm_term"
    static let utmContent = "utm_content"
  }
}
