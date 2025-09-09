//
//  DeepLinkROuter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

// swiftlint: disable all
import SwiftUI

@MainActor
final class DeepLinkRouter: ObservableObject {
  static let shared = DeepLinkRouter()

  @Published var pendingDestination: Destination?

  enum Destination: Equatable {
    case profileAfterUpload
    case openVideo(postId: UInt32, principal: String, canisterId: String?)
  }

  @discardableResult func resolve(from userInfo: [AnyHashable: Any]) -> Destination? {
    if let dest = resolveUploadDestination(fromPNs: userInfo) {
      pendingDestination = dest
    } else if let dest = resolveBranchDestination(fromBranch: userInfo) {
      pendingDestination = dest
    }
    return pendingDestination
  }

  private func resolveUploadDestination(fromPNs userInfo: [AnyHashable: Any]) -> Destination? {
    guard
      let payloadString = userInfo[Constants.payloadString] as? String,
      let data = payloadString.data(using: .utf8),
      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
      let type = json[Constants.typeString] as? String
    else {
      return nil
    }

    if type == Constants.videoUploadSuccessType {
      return .profileAfterUpload
    }
    return nil
  }

  private func resolveBranchDestination(fromBranch params: [AnyHashable: Any]) -> Destination? {
    guard DeepLinkRouter.isBranchClick(params["+clicked_branch_link"]) else { return nil }

    if let type = params[Constants.typeString] as? String,
       type == Constants.videoUploadSuccessType {
      return .profileAfterUpload
    }

    let postIdString =
    DeepLinkRouter.string(from: params["post_id"]) ??
    DeepLinkRouter.string(from: params["video_id"]) ??
    DeepLinkRouter.postIdFromDeeplinkPath(DeepLinkRouter.string(from: params["$deeplink_path"]))

    guard let postIdStr = postIdString, let postId = UInt32(postIdStr) else { return nil }

    let principal =
    DeepLinkRouter.string(from: params["principal"]) ??
    DeepLinkRouter.string(from: params["principal_id"])

    guard let principalStr = principal, !principalStr.isEmpty else { return nil }

    let canisterId = DeepLinkRouter.string(from: params["canister_id"])

    return .openVideo(postId: postId, principal: principalStr, canisterId: canisterId)
  }
}

private extension DeepLinkRouter {
  static func isBranchClick(_ value: Any?) -> Bool {
    switch value {
    case let n as NSNumber: return n.intValue == 1
    case let s as String:   return s == "1" || s.lowercased() == "true"
    case let b as Bool:     return b
    default:                return false
    }
  }
  
  static func string(from any: Any?) -> String? {
    if let s = any as? String { return s }
    if let n = any as? NSNumber { return n.stringValue }
    return nil
    // (Branch normally gives strings/NSNumber.)
  }
  
  static func postIdFromDeeplinkPath(_ path: String?) -> String? {
    guard let path = path, path.hasPrefix("/video/") else { return nil }
    // Accept forms like "/video/123" or "/video/123/"
    let comps = path.split(separator: "/")
    // Expected: ["", "video", "123"] or ["video", "123"] depending on leading "/"
    if let last = comps.last, CharacterSet.decimalDigits.isSuperset(of: CharacterSet(charactersIn: String(last))) {
      return String(last)
    }
    return nil
  }
}

extension DeepLinkRouter {
  enum Constants {
    static let payloadString = "payload"
    static let typeString = "type"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
    static let branchParameters = "Branch parameters:"
    static let branchError = "Branch error:"
  }
}
// swiftlint: enable all
