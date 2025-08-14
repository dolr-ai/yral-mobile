//
//  NotificationsToggleViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import UserNotifications
import UIKit
import iosSharedUmbrella

@MainActor
class NotificationToggleViewModel: ObservableObject {
  @Published var isNotificationEnabled: Bool = false

  init() {
    refreshStatus()
  }

  func refreshStatus() {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      DispatchQueue.main.async {
        self.isNotificationEnabled = settings.authorizationStatus == .authorized
      }
    }
  }

  func togglePermission(to newValue: Bool) {
    if newValue {
      UNUserNotificationCenter.current().getNotificationSettings { settings in
        switch settings.authorizationStatus {
        case .notDetermined:
          UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
              DispatchQueue.main.async {
                AnalyticsModuleKt.getAnalyticsManager().trackEvent(
                  event: PushNotificationsEnabledEventData()
                )
                self.isNotificationEnabled = granted
                if granted {
                  UIApplication.shared.registerForRemoteNotifications()
                }
              }
            }
        case .denied:
          self.openSettings()
        case .authorized, .provisional, .ephemeral:
          DispatchQueue.main.async {
            self.isNotificationEnabled = true
          }
        @unknown default:
          break
        }
      }
    } else {
      openSettings()
    }
  }

  private func openSettings() {
    guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else { return }
    if UIApplication.shared.canOpenURL(settingsUrl) {
      DispatchQueue.main.async {
        UIApplication.shared.open(settingsUrl)
      }
    }
  }
}
