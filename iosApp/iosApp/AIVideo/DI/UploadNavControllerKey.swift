//
//  UploadNavControllerKey.swift
//  iosApp
//
//  Created by Samarth Paboowal on 01/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

private struct UploadNavControllerKey: EnvironmentKey {
  static let defaultValue: UINavigationController? = nil
}

extension EnvironmentValues {
  var uploadNavController: UINavigationController? {
    get { self[UploadNavControllerKey.self] }
    set { self[UploadNavControllerKey.self] = newValue }
  }
}
