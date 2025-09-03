//
//  AppUpdateHandler.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import UIKit
import iosSharedUmbrella

final class AppUpdateHandler: Sendable {
  static let shared = AppUpdateHandler()
  private init() {}

  func getAppUpdateStatus() -> AppUpdateType {
    let flagManager = AppDIHelper().getFeatureFlagManager()
    guard let config = flagManager.get(flag: AppFeatureFlags.Ios.shared.InAppUpdate) as? IAPConfig else { return .none }
    let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    if self.isLower(currentVersion, than: config.minSupportedVersion) {
      return .force
    } else if self.isLower(currentVersion, than: config.recommendedVersion) {
      return .recommended
    } else {
      return .none
    }
  }

  private func isLower(_ currentVersion: String, than policyVersion: String) -> Bool {
    let currentArray = currentVersion.split(separator: ".").map { Int($0) ?? .zero }
    let policyArray = policyVersion.split(separator: ".").map { Int($0) ?? .zero }
    let versionParts = max(currentArray.count, policyArray.count)
    for version in .zero..<versionParts {
      let currentVersionPart = version < currentArray.count ? currentArray[version] : .zero
      let policyVersionPart = version < policyArray.count ? policyArray[version] : .zero
      if currentVersionPart != policyVersionPart { return currentVersionPart < policyVersionPart }
    }
    return false
  }

  static func redirectToAppStore() {
    let appID = Constants.appStoreID

    let urlStr = "itms-apps://itunes.apple.com/app/id\(appID)"
    guard let url = URL(string: urlStr) else { return }

    if UIApplication.shared.canOpenURL(url) {
      UIApplication.shared.open(url, options: [:], completionHandler: nil)
    }
  }

  enum AppUpdateType {
    case none
    case force
    case recommended
  }
}

extension AppUpdateHandler {
  enum Constants {
    static let updateKey = "app_update_ios"
    static let appStoreID = "6740337368"
  }
}
