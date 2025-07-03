//
//  FirebaseNotifications.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 02/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import FirebaseMessaging

class FirebaseNotifications: NotificationService {
  func getRegistrationToken() -> String {
    Messaging.messaging().fcmToken ?? ""
  }
}
