//
//  ShareVideoHandler.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import BranchSDK

struct SharePayload {
  let title: String
  let description: String
  let imageUrl: String?
  let postId: String
  let principalId: String
  let canisterId: String?
}

final class BranchShareService {
  static let shared = BranchShareService()

  func makeBranchUniversalObject(for payload: SharePayload) -> BranchUniversalObject {
    let branchUniversalObject = BranchUniversalObject()
    branchUniversalObject.canonicalUrl = "https://yral.app/video/\(payload.postId)"
    branchUniversalObject.title = payload.title
    branchUniversalObject.contentDescription = payload.description
    if let imageUrl = payload.imageUrl { branchUniversalObject.imageUrl = imageUrl }

    branchUniversalObject.contentMetadata.customMetadata["post_id"] = payload.postId
    branchUniversalObject.contentMetadata.customMetadata["principal_id"] = payload.principalId
    if let canisterId = payload.canisterId {
      branchUniversalObject.contentMetadata.customMetadata["canister_id"] = canisterId
    }
    return branchUniversalObject
  }

  func linkProperties(payload: SharePayload) -> BranchLinkProperties {
    let linkProperties = BranchLinkProperties()
    linkProperties.feature = "share"
    linkProperties.tags = ["organic", "user_share"]
    if let imageUrl = payload.imageUrl {
      linkProperties.addControlParam("$og_image_url", withValue: imageUrl)
    }
    linkProperties.addControlParam("$og_title", withValue: payload.title)
    linkProperties.addControlParam(
      "$og_description", withValue: payload.description
    )
    linkProperties.addControlParam("$deeplink_path", withValue: "/video/\(payload.postId)")
    return linkProperties
  }

  func generateLink(payload: SharePayload,
                    channel: String,
                    campaign: String? = nil,
                    completion: @escaping (String?) -> Void) {
    let branchUniversalObject = makeBranchUniversalObject(for: payload)
    let properties = linkProperties(payload: payload)

    branchUniversalObject.getShortUrl(with: properties) { url, error in
      if let error = error {
        print("Branch link error:", error)
        completion(nil)
      } else {
        completion(url)
      }
    }
  }
}
