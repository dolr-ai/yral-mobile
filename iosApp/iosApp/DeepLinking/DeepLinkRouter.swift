//
//  DeepLinkROuter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import iosSharedUmbrella

@MainActor
final class DeepLinkRouter: ObservableObject {
  static let shared = DeepLinkRouter()

  @Published var pendingDestination: Destination?

  enum Destination: Equatable {
    case home
    case leaderboard
    case addVideo
    case aiGen
    case wallet
    case profile
    case profileAfterUpload
    case openVideo(postId: String, canisterId: String?)
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
    } else if let deepLinkPath = params["$deeplink_path"] as? String {
      let route = AppDIHelper().getRoutingService().parseUrl(url: deepLinkPath)
      return mapRouteToDestination(route)
    }

    return nil
  }

  private func mapRouteToDestination(_ route: AppRoute) -> Destination? {
    switch route {
    case let postDetailsRoute as PostDetailsRoute:
      return .openVideo(postId: postDetailsRoute.postId, canisterId: postDetailsRoute.canisterId)
    case _ as Home:
      return .home
    case _ as Leaderboard:
      return .leaderboard
    case _ as AddVideo:
      return .addVideo
    case _ as GenerateAIVideo:
      return .aiGen
    case _ as Wallet:
      return .wallet
    case _ as Profile:
      return .profile
    default:
      return nil
    }
  }
}

private extension DeepLinkRouter {
  static func isBranchClick(_ value: Any?) -> Bool {
    switch value {
    case let linkNumber as NSNumber: return linkNumber.intValue == 1
    case let linkString as String: return linkString == "1" || linkString.lowercased() == "true"
    case let linkBool as Bool: return linkBool
    default: return false
    }
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
