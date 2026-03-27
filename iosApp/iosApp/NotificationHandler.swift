import Foundation
import iosSharedUmbrella

enum NotificationHandler {
  struct Config {
    let fallbackInternalUrl: (() -> String?)?
    let fallbackRoute: (() -> AppRoute)?
    let onResolve: (() -> Void)?
    let navigateDirectlyInForeground: Bool

    init(
      fallbackInternalUrl: (() -> String?)? = nil,
      fallbackRoute: (() -> AppRoute)? = nil,
      onResolve: (() -> Void)? = nil,
      navigateDirectlyInForeground: Bool = false
    ) {
      self.fallbackInternalUrl = fallbackInternalUrl
      self.fallbackRoute = fallbackRoute
      self.onResolve = onResolve
      self.navigateDirectlyInForeground = navigateDirectlyInForeground
    }
  }

  enum Constants {
    static let payloadString = "payload"
    static let typeString = "type"
    static let internalURL = "internalUrl"
    static let deeplinkPath = "$deeplink_path"
    static let branchClick = "+clicked_branch_link"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
    static let draftCreatedType = "VideoUploadedToDraft"
    static let rewardEarnedType = "RewardEarned"
    static let viewDraftsCta = "View Drafts"
  }

  static func notificationRoute(
    from userInfo: [AnyHashable: Any],
    parseUrl: (String) -> AppRoute = defaultParseUrl
  ) -> AppRoute? {
    guard let payload = notificationPayload(from: userInfo) else { return nil }
    return notificationRoute(from: payload, parseUrl: parseUrl)
  }

  static func foregroundRoute(
    from userInfo: [AnyHashable: Any],
    parseUrl: (String) -> AppRoute = defaultParseUrl
  ) -> AppRoute? {
    guard let payload = notificationPayload(from: userInfo) else { return nil }
    guard config(for: payload)?.navigateDirectlyInForeground == true else { return nil }
    return notificationRoute(from: payload, parseUrl: parseUrl)
  }

  static func notificationType(from userInfo: [AnyHashable: Any]) -> String? {
    notificationPayload(from: userInfo)?[Constants.typeString] as? String
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

  static func handleForegroundNotification(
    userInfo: [AnyHashable: Any],
    title: String?,
    body: String?,
    navigate: @escaping (AppRoute?) -> Void
  ) {
    if let route = foregroundRoute(from: userInfo) {
      navigate(route)
      return
    }

    if notificationType(from: userInfo) == Constants.draftCreatedType {
      VideoGenerationTracker.shared.onDraftCreatedAndRequestDraftsTab()
      ForegroundNotificationToastBridgeKt.showForegroundNotificationSuccessToastWithAction(
        title: title,
        body: body,
        actionText: Constants.viewDraftsCta
      ) {
        navigate(notificationRoute(from: userInfo))
      }
      return
    }

    ForegroundNotificationToastBridgeKt.showForegroundNotificationSuccessToast(
      title: title,
      body: body
    )
  }

  private static let configByType: [String: Config] = [
    Constants.videoUploadSuccessType: Config(
      fallbackRoute: { VideoUploadSuccessful(videoID: nil) }
    ),
    Constants.draftCreatedType: Config(
      fallbackInternalUrl: profileUrl,
      fallbackRoute: { defaultParseUrl(AppRouteKt.profilePath()) },
      onResolve: {
        VideoGenerationTracker.shared.onDraftCreatedAndRequestDraftsTab()
      }
    ),
    Constants.rewardEarnedType: Config(
      navigateDirectlyInForeground: true
    )
  ]

  private static func defaultParseUrl(_ url: String) -> AppRoute {
    AppDIHelper().getRoutingService().parseUrl(url: url)
  }

  private static func notificationRoute(
    from payload: [String: Any],
    parseUrl: (String) -> AppRoute
  ) -> AppRoute? {
    let config = config(for: payload)
    config?.onResolve?()

    if let internalURL = payload[Constants.internalURL] as? String {
      return parseUrl(internalURL)
    }

    return config?.fallbackRoute?()
  }

  private static func config(for payload: [String: Any]) -> Config? {
    guard let type = payload[Constants.typeString] as? String else {
      return nil
    }
    return configByType[type]
  }

  private static func profileUrl() -> String? {
    AppRouteKt.profilePath()
  }

  private static func notificationPayload(from userInfo: [AnyHashable: Any]) -> [String: Any]? {
    if let payloadString = userInfo[Constants.payloadString] as? String,
       let payloadData = payloadString.data(using: .utf8),
       let rawPayload = try? JSONSerialization.jsonObject(with: payloadData),
       let payload = rawPayload as? [String: Any] {
      return payload
    }

    var fields = userInfo.reduce(into: [String: Any]()) { result, entry in
      guard let key = entry.key as? String else {
        return
      }
      result[key] = entry.value
    }

    let type = fields[Constants.typeString] as? String
    if let type,
       fields[Constants.internalURL] == nil,
       let fallbackInternalUrl = configByType[type]?.fallbackInternalUrl?() {
      fields[Constants.internalURL] = fallbackInternalUrl
    }

    return fields.isEmpty ? nil : fields
  }
}

extension String {
  var nilIfEmpty: String? {
    isEmpty ? nil : self
  }
}
