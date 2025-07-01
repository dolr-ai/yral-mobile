//
//  FirebaseNotifications.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 02/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import FirebaseMessaging

class FirebaseNotifications: NotificationService {
  func getRegistrationToken() async throws -> String {
    try await Messaging.messaging().token()
  }
}
