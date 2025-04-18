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
    let webView = WKWebView()
    webView.navigationDelegate = context.coordinator
    webView.uiDelegate        = context.coordinator
    webView.allowsBackForwardNavigationGestures = false
    webView.load(URLRequest(url: url))
    return webView
  }

  func updateUIView(_ uiView: WKWebView, context: Context) { }

  func makeCoordinator() -> Coordinator { Coordinator(allowed: url) }

  final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
    private let allowedURL: URL

    init(allowed: URL) { self.allowedURL = allowed }

    func webView(
      _ webView: WKWebView,
      decidePolicyFor action: WKNavigationAction,
      decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
      if action.request.url == allowedURL {
        decisionHandler(.allow)
      } else {
        decisionHandler(.cancel)
      }
    }

    func webView(
      _ webView: WKWebView,
      didFinish navigation: WKNavigation!
    ) {

      let jsScript = """
      /* Hide typical bottom–nav elements */
      (function(){
        const selectors = [
          'nav',                       /* generic <nav> tag   */
          '.bottom-nav', '#bottomNav', /* common class/id     */
          'footer'                     /* many sites use <footer> */
        ];
        selectors.forEach(sel => {
          document.querySelectorAll(sel).forEach(el => {
            el.style.display = 'none';             // remove from layout
            el.style.pointerEvents = 'none';       // extra safety
          });
        });
      })();
      """
      webView.evaluateJavaScript(jsScript, completionHandler: nil)
    }

    func webView(
      _ webView: WKWebView,
      createWebViewWith configuration: WKWebViewConfiguration,
      for navigationAction: WKNavigationAction,
      windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
      nil
    }
  }
}
