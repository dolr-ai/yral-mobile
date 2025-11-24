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

  @Published private(set) var appRoute: AppRoute?
  @Published private(set) var pendingDestination: Destination?

  enum Destination: Equatable {
    case home
    case leaderboard
    case addVideo
    case aiGen
    case wallet
    case profile
    case profileAfterUpload
    case openVideo(postId: String, canisterId: String?)
    case videoViewedRewards(videoID: String, totalViews: Int64, rewardAmount: Double)
  }

  @discardableResult func resolve(from userInfo: [AnyHashable: Any]) -> Destination? {
    if let route = resolveAppRoute(fromPNs: userInfo) {
      setRoute(route: route)
    } else if let route = resolveBranchAppRoute(fromBranch: userInfo) {
      setRoute(route: route)
    }
    return pendingDestination
  }
  
  func setRoute(route: AppRoute) {
    appRoute = route
    pendingDestination = mapRouteToDestination(route)
  }
    
  func clearResolution() {
    appRoute = nil
    pendingDestination = nil
  }

  private func resolveAppRoute(fromPNs userInfo: [AnyHashable: Any]) -> AppRoute? {
    guard let payloadString = userInfo[Constants.payloadString] as? String,
          let payloadData = payloadString.data(using: .utf8),
          let payloadDict = try? JSONSerialization.jsonObject(with: payloadData) as? [String: Any],
          let internalURL = payloadDict[Constants.internalURL] as? String else {
      return nil
    }

    let route = AppDIHelper().getRoutingService().parseUrl(url: internalURL)
    return route
  }

  private func resolveBranchAppRoute(fromBranch params: [AnyHashable: Any]) -> AppRoute? {
    guard DeepLinkRouter.isBranchClick(params["+clicked_branch_link"]) else { return nil }

    if let type = params[Constants.typeString] as? String,
       type == Constants.videoUploadSuccessType {
      return VideoUploadSuccessful(videoID: nil)
    } else if let deepLinkPath = params["$deeplink_path"] as? String {
      let route = AppDIHelper().getRoutingService().parseUrl(url: deepLinkPath)
      return route
    }

    return nil
  }

  private func mapRouteToDestination(_ route: AppRoute) -> Destination? {
    switch route {
    case let postDetailsRoute as PostDetailsRoute:
      return .openVideo(
        postId: postDetailsRoute.postId,
        canisterId: postDetailsRoute.canisterId
      )
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
    case let rewardsReceived as RewardsReceived:
      return .videoViewedRewards(
        videoID: rewardsReceived.videoID ?? "",
        totalViews: Int64(rewardsReceived.viewCount ?? "0") ?? 0,
        rewardAmount: Double(rewardsReceived.rewardBtc ?? "0") ?? 0
      )
    case _ as VideoUploadSuccessful:
      return .profileAfterUpload
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
    static let internalURL = "internalUrl"
    static let videoUploadSuccessType = "VideoUploadSuccessful"
    static let branchParameters = "Branch parameters:"
    static let branchError = "Branch error:"
  }
}
