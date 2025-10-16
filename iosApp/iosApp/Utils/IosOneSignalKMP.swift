//
//  IosOneSignalKMP.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import OneSignalFramework
import iosSharedUmbrella

class IosOneSignalKMP: OneSignalKMP {
  func initialize(appId: String) {
    OneSignal.initialize(appId)
  }

  func login(externalId: String) {
    OneSignal.login(externalId)
  }

  func logout() {
    OneSignal.logout()
  }
}
