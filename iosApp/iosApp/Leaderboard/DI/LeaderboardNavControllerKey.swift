//
//  LeaderboardNavControllerKey.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

private struct LeaderboardNavControllerKey: EnvironmentKey {
  static let defaultValue: UINavigationController? = nil
}

extension EnvironmentValues {
  var leaderboardNavController: UINavigationController? {
    get { self[LeaderboardNavControllerKey.self] }
    set { self[LeaderboardNavControllerKey.self] = newValue }
  }
}
