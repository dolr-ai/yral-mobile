//
//  VideoPickerViewControllerRepresentable.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import AVKit

struct VideoPickerViewControllerRepresentable: UIViewControllerRepresentable {
  @Binding var videoURL: URL?
  @Environment(\.presentationMode) var presentationMode

  func makeUIViewController(context: Context) -> VideoPickerViewController {
    let viewController = VideoPickerViewController()
    viewController.delegate = context.coordinator
    viewController.modalPresentationStyle = .overFullScreen
    return viewController
  }

  func updateUIViewController(_ uiViewController: VideoPickerViewController, context: Context) {
  }

  func makeCoordinator() -> Coordinator {
    Coordinator(self)
  }

  class Coordinator: NSObject, VideoPickerDelegate {
    var parent: VideoPickerViewControllerRepresentable

    init(_ parent: VideoPickerViewControllerRepresentable) {
      self.parent = parent
    }

    func videoPicker(_ picker: VideoPickerViewController, didPickVideo url: URL) {
      DispatchQueue.main.async {
        self.parent.videoURL = url
        self.parent.presentationMode.wrappedValue.dismiss()
      }
    }

    func cancelled() {
      DispatchQueue.main.async {
        self.parent.presentationMode.wrappedValue.dismiss()
      }
    }
  }
}
