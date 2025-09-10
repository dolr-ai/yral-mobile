//
//  ViewControllerWrapper.swift
//  iosApp
//
//  Created by Samarth Paboowal on 01/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ViewControllerWrapper: UIViewControllerRepresentable {
  let controller: UIViewController

  func makeUIViewController(context: Context) -> UIViewController {
    controller
  }

  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
