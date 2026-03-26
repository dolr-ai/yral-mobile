import Testing
@testable import Yral
import iosSharedUmbrella

struct YralTests {

  @Test
  func branchClickHandlesTruthyValues() {
    #expect(AppRouteResolver.isBranchClick(true))
    #expect(AppRouteResolver.isBranchClick(NSNumber(value: 1)))
    #expect(AppRouteResolver.isBranchClick("true"))
    #expect(AppRouteResolver.isBranchClick("1"))
    #expect(!AppRouteResolver.isBranchClick(false))
    #expect(!AppRouteResolver.isBranchClick("false"))
    #expect(!AppRouteResolver.isBranchClick(nil))
  }

  @Test
  func branchVideoUploadRouteDoesNotRequireRoutingService() {
    var didParseURL = false
    let route = AppRouteResolver.branchRoute(
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
    let route = AppRouteResolver.notificationRoute(
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
}
