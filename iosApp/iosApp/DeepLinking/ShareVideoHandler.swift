//
//  ShareVideoHandler.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import BranchSDK
import iosSharedUmbrella

struct SharePayload {
  let title: String
  let description: String
  let imageUrl: String?
  let postId: String
  let canisterId: String
}

final class BranchShareService {
  static let shared = BranchShareService()

  func makeBranchUniversalObject(for payload: SharePayload, deeplinkURL: String) -> BranchUniversalObject {
    let branchUniversalObject = BranchUniversalObject()
    branchUniversalObject.title = payload.title
    branchUniversalObject.contentDescription = payload.description
//    if let imageUrl = payload.imageUrl { branchUniversalObject.imageUrl = imageUrl }
    branchUniversalObject.imageUrl = nil
    branchUniversalObject.contentMetadata.customMetadata["internal_url"] = deeplinkURL
    return branchUniversalObject
  }

  func linkProperties(payload: SharePayload, deeplinkURL: String) -> BranchLinkProperties {
    let linkProperties = BranchLinkProperties()
    linkProperties.feature = "share"
    linkProperties.tags = ["organic", "user_share"]
    linkProperties.addControlParam(
      "$deeplink_path",
      withValue: deeplinkURL.components(separatedBy: "://").last ?? deeplinkURL
    )
    return linkProperties
  }

  func generateLink(payload: SharePayload,
                    channel: String,
                    campaign: String? = nil,
                    completion: @escaping (String?, Error?) -> Void) {
    let route = PostDetailsRoute(canisterId: payload.canisterId, postId: payload.postId)
    guard let url = AppDIHelper().getRoutingService().buildUrl(route: route) else {
      completion(nil, DeepLinkUrlError.notFound)
      return
    }
    let branchUniversalObject = makeBranchUniversalObject(for: payload, deeplinkURL: url)
    let properties = linkProperties(payload: payload, deeplinkURL: url)

    branchUniversalObject.getShortUrl(with: properties) { url, error in
      if let error = error {
        print("Branch link error:", error)
        completion(nil, error)
      } else {
        completion(url, nil)
      }
    }
  }

  enum DeepLinkUrlError: Error {
    case notFound
  }
}
