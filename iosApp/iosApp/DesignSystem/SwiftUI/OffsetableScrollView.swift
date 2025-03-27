//
//  OffsetableScrollView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 27/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct OffsettableScrollView<Content: View>: UIViewRepresentable {
  @Binding var contentOffset: CGPoint
  let content: Content

  init(contentOffset: Binding<CGPoint>, @ViewBuilder content: () -> Content) {
    self._contentOffset = contentOffset
    self.content = content()
  }

  func makeCoordinator() -> Coordinator {
    Coordinator(self)
  }

  func makeUIView(context: Context) -> UIScrollView {
    let scrollView = UIScrollView()
    scrollView.delegate = context.coordinator
    scrollView.showsVerticalScrollIndicator = true
    scrollView.alwaysBounceVertical = true

    let hostingController = UIHostingController(rootView: content)
    hostingController.view.translatesAutoresizingMaskIntoConstraints = false
    hostingController.view.backgroundColor = .clear
    scrollView.addSubview(hostingController.view)

    NSLayoutConstraint.activate([
      hostingController.view.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
      hostingController.view.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
      hostingController.view.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
      hostingController.view.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
      hostingController.view.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor)
    ])

    return scrollView
  }

  func updateUIView(_ uiView: UIScrollView, context: Context) {
    if uiView.contentOffset != contentOffset {
      uiView.setContentOffset(contentOffset, animated: false)
    }
  }

  class Coordinator: NSObject, UIScrollViewDelegate {
    var parent: OffsettableScrollView

    init(_ parent: OffsettableScrollView) {
      self.parent = parent
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
      DispatchQueue.main.async {
        self.parent.contentOffset = scrollView.contentOffset
      }
    }
  }
}
