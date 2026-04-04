import Testing
@testable import Yral
import iosSharedUmbrella

struct YralTests {
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
}
