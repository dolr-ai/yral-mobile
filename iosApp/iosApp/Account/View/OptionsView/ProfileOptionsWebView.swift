//
//  ShareOptionsWebView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import WebKit

struct ProfileOptionsWebView: UIViewRepresentable {
  let url: URL

  func makeUIView(context: Context) -> WKWebView {
    WKWebView()
  }

  func updateUIView(_ uiView: WKWebView, context: Context) {
    let request = URLRequest(url: url)
    uiView.load(request)
  }
}
