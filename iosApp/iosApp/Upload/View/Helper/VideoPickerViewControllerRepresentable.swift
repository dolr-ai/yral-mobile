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
  let viewModel: UploadViewModel

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
    Coordinator(self, viewModel: viewModel)
  }

  class Coordinator: NSObject, VideoPickerDelegate {
    let parent: VideoPickerViewControllerRepresentable
    let viewModel: UploadViewModel

    init(_ parent: VideoPickerViewControllerRepresentable,
         viewModel: UploadViewModel) {
      self.parent = parent
      self.viewModel = viewModel
    }

    func videoPicker(_ picker: VideoPickerViewController, didPickVideo url: URL) {
      DispatchQueue.main.async {
        self.viewModel.handleVideoPicked(url)
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
