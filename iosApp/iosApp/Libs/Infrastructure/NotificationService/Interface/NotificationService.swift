//
//  NotificationService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 02/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol NotificationService {
  func getRegistrationToken() async throws -> String
}
