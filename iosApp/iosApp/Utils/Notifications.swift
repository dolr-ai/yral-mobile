//
//  Notifications.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension Notification.Name {
  static let eulaAcceptedChanged = Notification.Name("yral.eulaAcceptedChanged")
  static let feedItemReady = Notification.Name("yral.feedItemReady")
  static let appAuthenticated = Notification.Name("yral.appAuthenticated")
  static let registrationTokenUpdated = Notification.Name("yral.registrationTokenUpdated")
  static let videoUploadNotificationReceived = Notification.Name("yral.videoUploadNotificationReceived")
}
