import Foundation
import Testing
@testable import Yral
import iosSharedUmbrella

struct YralTests {
  private static let repoRoot = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent()
    .deletingLastPathComponent()
    .deletingLastPathComponent()

  private static func repoText(_ relativePath: String) throws -> String {
    try String(contentsOf: repoRoot.appendingPathComponent(relativePath), encoding: .utf8)
  }

  private static func repoPath(_ relativePath: String) -> String {
    repoRoot.appendingPathComponent(relativePath).path
  }

  private static func repoJSON(_ relativePath: String) throws -> [String: Any] {
    let data = try Data(contentsOf: repoRoot.appendingPathComponent(relativePath))
    let object = try JSONSerialization.jsonObject(with: data)
    guard let dictionary = object as? [String: Any] else {
      fatalError("Expected JSON object at \(relativePath)")
    }
    return dictionary
  }

  @Test
  func branchClickHandlesTruthyValues() {
    #expect(NotificationHandler.isBranchClick(true))
    #expect(NotificationHandler.isBranchClick(NSNumber(value: 1)))
    #expect(NotificationHandler.isBranchClick("true"))
    #expect(NotificationHandler.isBranchClick("1"))
    #expect(!NotificationHandler.isBranchClick(false))
    #expect(!NotificationHandler.isBranchClick("false"))
    #expect(!NotificationHandler.isBranchClick(nil))
  }

  @Test
  func branchVideoUploadRouteDoesNotRequireRoutingService() {
    var didParseURL = false
    let route = NotificationHandler.branchRoute(
      from: [
        "+clicked_branch_link": true,
        "type": "VideoUploadSuccessful"
      ]
    ) { _ in
      didParseURL = true
      return VideoUploadSuccessful(videoID: "unexpected")
    }

    #expect(route != nil)
    #expect(!didParseURL)
  }

  @Test
  func notificationRoutePassesInternalURLToParser() {
    var parsedURL: String?
    let route = NotificationHandler.notificationRoute(
      from: [
        "payload": """
        {"internalUrl":"https://yral.com/profile"}
        """
      ]
    ) { url in
      parsedURL = url
      return VideoUploadSuccessful(videoID: nil)
    }

    #expect(route != nil)
    #expect(parsedURL == "https://yral.com/profile")
  }

  @Test
  func draftNotificationWithoutPayloadAddsProfileFallbackBeforeParsing() {
    var parsedURL: String?
    let route = NotificationHandler.notificationRoute(
      from: [
        "type": "VideoUploadedToDraft",
        "post_id": "123"
      ]
    ) { url in
      parsedURL = url
      return VideoUploadSuccessful(videoID: nil)
    }

    #expect(route != nil)
    #expect(parsedURL == AppRouteKt.profilePath())
  }

  @Test
  func rewardEarnedForegroundRouteNavigatesDirectly() {
    var parsedURL: String?
    let route = NotificationHandler.foregroundRoute(
      from: [
        "payload": """
        {"type":"RewardEarned","internalUrl":"wallet/rewards"}
        """
      ]
    ) { url in
      parsedURL = url
      return VideoUploadSuccessful(videoID: nil)
    }

    #expect(route != nil)
    #expect(parsedURL == "wallet/rewards")
  }

  @Test
  func draftNotificationForegroundRouteDoesNotNavigateDirectly() {
    let route = NotificationHandler.foregroundRoute(
      from: [
        "payload": """
        {"type":"VideoUploadedToDraft","internalUrl":"\(AppRouteKt.profilePath())"}
        """
      ]
    ) { _ in
      VideoUploadSuccessful(videoID: nil)
    }

    #expect(route == nil)
  }

  @Test
  func notificationTypeReadsTopLevelDataWhenPayloadIsMissing() {
    let type = NotificationHandler.notificationType(
      from: [
        "type": "VideoUploadedToDraft",
        "post_id": "123"
      ]
    )

    #expect(type == "VideoUploadedToDraft")
  }

  func podfileMatchesTheRemainingCompiledImports() throws {
    let podfile = try Self.repoText("iosApp/Podfile")

    #expect(podfile.contains("pod 'FirebaseCore'"))
    #expect(podfile.contains("pod 'FirebaseAppCheck'"))
    #expect(podfile.contains("pod 'FirebaseMessaging'"))
    #expect(podfile.contains("pod 'FBSDKCoreKit'"))
    #expect(!podfile.contains("pod 'FirebaseAnalytics'"))
    #expect(!podfile.contains("pod 'FirebaseCrashlytics'"))
    #expect(!podfile.contains("pod 'FirebasePerformance'"))
    #expect(!podfile.contains("pod 'FirebaseInstallations'"))
    #expect(!podfile.contains("pod 'FirebaseFirestore'"))
    #expect(!podfile.contains("pod 'FirebaseStorage'"))
    #expect(!podfile.contains("pod 'FirebaseAuth'"))
    #expect(!podfile.contains("pod 'FirebaseInAppMessaging'"))
    #expect(!podfile.contains("pod 'Mixpanel'"))
    #expect(!podfile.contains("pod 'Sentry'"))
  }

  @Test
  func iosWrapperContainsOnlyTheRemainingCompileSourceSwiftFiles() throws {
    let expectedPaths = Set([
      "iosApp/iosApp/IosApp.swift",
      "iosApp/iosApp/NotificationHandler.swift",
      "iosApp/iosApp/RootView.swift",
      "iosApp/iosApp/Utils/AppCheckProviderFactory.swift",
      "iosApp/iosApp/Libs/Infrastructure/Auth/Helpers/KeychainHelper.swift"
    ])

    let swiftFiles = Set(
      try FileManager.default.subpathsOfDirectory(atPath: Self.repoPath("iosApp/iosApp"))
        .filter { $0.hasSuffix(".swift") }
        .map { "iosApp/iosApp/\($0)" }
    )

    #expect(swiftFiles == expectedPaths)
  }

  @Test
  func iosWrapperNoLongerContainsLegacyGrpcArtifacts() throws {
    let project = try Self.repoText("iosApp/iosApp.xcodeproj/project.pbxproj")

    #expect(!project.contains("ml_feed.proto"))
    #expect(!project.contains("/* grpc */"))
    #expect(
      !FileManager.default.fileExists(
        atPath: Self.repoPath("iosApp/iosApp/Feed/Data/DTO/grpc/ml_feed.proto")
      )
    )
  }

  @Test
  func iosProjectNoLongerReferencesRemovedCrashlyticsUploadScripts() throws {
    let project = try Self.repoText("iosApp/iosApp.xcodeproj/project.pbxproj")

    #expect(!project.contains("FirebaseCrashlytics/run"))
    #expect(!project.contains("ACB815362CF9CEF600104B7B /* ShellScript */"))
    #expect(!project.contains("ACD941B42CFF575C0038FCB6 /* Run Script */"))
  }

  @Test
  func swiftPackageResolutionKeepsOnlyBranch() throws {
    let workspaceResolved = try Self.repoJSON("iosApp/iosApp.xcworkspace/xcshareddata/swiftpm/Package.resolved")
    let projectResolved = try Self.repoJSON(
      "iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
    )

    let workspacePins = (workspaceResolved["pins"] as? [[String: Any]]) ?? []
    let projectPins = (projectResolved["pins"] as? [[String: Any]]) ?? []

    #expect(workspacePins.count == 1)
    #expect(projectPins.count == 1)
    #expect(workspacePins.first?["location"] as? String == "https://github.com/BranchMetrics/ios-branch-sdk-spm")
    #expect(projectPins.first?["location"] as? String == "https://github.com/BranchMetrics/ios-branch-sdk-spm")
  }
}
