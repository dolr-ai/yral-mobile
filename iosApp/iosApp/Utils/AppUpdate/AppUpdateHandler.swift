//
//  AppUpdateHandler.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 03/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import FirebaseRemoteConfig

final class AppUpdateHandler: Sendable {
  static let shared = AppUpdateHandler()
  private init() {}

  func getAppUpdateStatus() async throws -> AppUpdateType {
    return try await withCheckedThrowingContinuation { continuation in
      RemoteConfig.remoteConfig().fetchAndActivate { _, error in
        guard error == nil else {
          continuation.resume(with: .failure(error!))
          return
        }
        do {
          let updateConfig = try RemoteConfig.remoteConfig()[Constants.updateKey].decoded(asType: AppUpdateDTO.self)
          let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
          if self.isLower(currentVersion, than: updateConfig.minSupportedVersion) {
            continuation.resume(with: .success(.force))
          } else if self.isLower(currentVersion, than: updateConfig.recommendedVersion) {
            continuation.resume(with: .success(.recommended))
          } else {
            continuation.resume(with: .success(.none))
          }
        } catch {
          print("Failed to decode configuration: \(error)")
          continuation.resume(throwing: error)
        }
      }
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

  enum AppUpdateType {
    case none
    case force
    case recommended
  }
}

extension AppUpdateHandler {
  enum Constants {
    static let updateKey = "app_update_ios"
  }
}
