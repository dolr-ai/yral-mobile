//
//  IosOneSignalKMP.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import iosSharedUmbrella

class IosDependencyProvider: ExternalDependencyProvider {
  func createOneSignalKMP() -> any OneSignalKMP {
    IosOneSignalKMP()
  }
}
