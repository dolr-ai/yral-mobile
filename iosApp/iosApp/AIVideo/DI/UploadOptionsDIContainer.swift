//
//  UploadOptionsDIContainer.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import SwiftUI

final class UploadOptionsDIContainer {
  func makeUploadOptionsView() -> UINavigationController {
    let rootView = UploadOptionsScreenView().environment(\.uploadNavController, nil)
    let host = UIHostingController(rootView: rootView)
    let navigationController = UINavigationController(rootViewController: host)
    host.rootView = UploadOptionsScreenView().environment(\.uploadNavController, navigationController)

    navigationController.setNavigationBarHidden(true, animated: false)
    navigationController.view.backgroundColor = .clear
    navigationController.edgesForExtendedLayout = .all
    host.edgesForExtendedLayout = .all
    host.extendedLayoutIncludesOpaqueBars = true

    return navigationController
  }
}
