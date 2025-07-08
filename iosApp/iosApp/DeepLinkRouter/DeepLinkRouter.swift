//
//  DeepLinkROuter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

final class DeepLinkRouter: ObservableObject {
  static let shared = DeepLinkRouter()
  @Published var pendingDestination: Destination?
  enum Destination {
    case profileAfterUpload
  }
}
