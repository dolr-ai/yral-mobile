//
//  AppDIContainerKey.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

private struct AppDIContainerKey: EnvironmentKey {
  static let defaultValue: AppDIContainer? = nil
}

extension EnvironmentValues {
  var appDIContainer: AppDIContainer? {
    get { self[AppDIContainerKey.self] }
    set { self[AppDIContainerKey.self] = newValue }
  }
}
