//
//  VideoPickerViewController.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 10/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit
import AVFoundation
import UniformTypeIdentifiers

protocol VideoPickerDelegate: AnyObject {
  func videoPicker(_ picker: VideoPickerViewController, didPickVideo url: URL)
  func cancelled()
}

class VideoPickerViewController: UIViewController {
  var videoMetadataProvider: VideoMetadataProvider = DefaultVideoMetadataProvider()
  weak var delegate: VideoPickerDelegate?

  private var didShowOptions = false

  override func viewDidLoad() {
    super.viewDidLoad()
    view.backgroundColor = .clear
    modalPresentationStyle = .overFullScreen
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    if !didShowOptions {
      didShowOptions = true
      showVideoPickerOptions()
    }
  }

  func showVideoPickerOptions() {
    let alert = UIAlertController(
      title: Constants.pickerOptionsTitle,
      message: nil,
      preferredStyle: .actionSheet
    )

    if UIImagePickerController.isSourceTypeAvailable(.camera) {
      alert.addAction(UIAlertAction(
        title: Constants.cameraActionTitle,
        style: .default,
        handler: { _ in self.openCamera() }
      ))
    }

    if UIImagePickerController.isSourceTypeAvailable(.photoLibrary) {
      alert.addAction(UIAlertAction(
        title: Constants.photoActionTitle,
        style: .default,
        handler: { _ in self.openPhotoLibrary() }
      ))
    }

    alert.addAction(UIAlertAction(
      title: Constants.fileManagerActionTitle,
      style: .default,
      handler: { _ in self.openDocumentPicker() }
    ))

    alert.addAction(UIAlertAction(
      title: Constants.cancelActionTitle,
      style: .cancel,
      handler: { _ in self.delegate?.cancelled() }
    ))

    self.present(alert, animated: true)
  }

  func openCamera() {
    let picker = UIImagePickerController()
    picker.mediaTypes = ["public.movie"]
    picker.delegate = self
    picker.sourceType = .camera
    picker.cameraCaptureMode = .video
    picker.videoMaximumDuration = Constants.videoMaxDuration
    self.present(picker, animated: true)
  }

  func openPhotoLibrary() {
    let picker = UIImagePickerController()
    picker.delegate = self
    picker.sourceType = .photoLibrary
    picker.mediaTypes = Constants.mediaTypes
    picker.modalPresentationStyle = .fullScreen
    self.present(picker, animated: true)
  }

  func openDocumentPicker() {
    let documentPicker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.movie])
    documentPicker.delegate = self
    self.present(documentPicker, animated: true)
  }

  func validateVideo(url: URL) -> Bool {
    guard let durationSeconds = videoMetadataProvider.duration(for: url) else {
      showAlert(title: Constants.errorAlertTitle,
                message: Constants.lengthViolationAlertMessage)
      return false
    }

    if durationSeconds > Constants.videoMaxDuration {
      showAlert(title: Constants.errorAlertTitle,
                message: Constants.lengthViolationAlertMessage)
      return false
    }

    guard let fileSize = videoMetadataProvider.fileSize(for: url) else {
      showAlert(title: Constants.errorAlertTitle,
                message: Constants.sizeViolationAlertMessage)
      return false
    }

    if fileSize > Constants.videoMaxFileSize {
      showAlert(title: Constants.errorAlertTitle,
                message: Constants.sizeViolationAlertMessage)
      return false
    }

    print("Video is valid. Duration: \(durationSeconds) seconds")
    return true
  }

  private func showAlert(title: String, message: String) {
    let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
    alert.addAction(UIAlertAction(title: Constants.okMessage, style: .default, handler: { [weak self] _ in
      self?.delegate?.cancelled()
    }))
    self.present(alert, animated: true)
  }
}

extension VideoPickerViewController: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
  func imagePickerController(_ picker: UIImagePickerController,
                             didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
    picker.dismiss(animated: true)
    if let mediaURL = info[.mediaURL] as? URL {
      if validateVideo(url: mediaURL) {
        delegate?.videoPicker(self, didPickVideo: mediaURL)
      }
    }
  }

  func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    delegate?.cancelled()
    picker.dismiss(animated: true)
  }
}

extension VideoPickerViewController: UIDocumentPickerDelegate {
  func documentPicker(_ controller: UIDocumentPickerViewController,
                      didPickDocumentsAt urls: [URL]) {
    controller.dismiss(animated: true)
    if let url = urls.first, validateVideo(url: url) {
      delegate?.videoPicker(self, didPickVideo: url)
    }
  }

  func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
    delegate?.cancelled()
    controller.dismiss(animated: true)
  }
}

extension VideoPickerViewController {
  enum Constants {
    static let pickerOptionsTitle = "Select Video Source"
    static let cameraActionTitle = "Camera"
    static let photoActionTitle = "Photo Library"
    static let fileManagerActionTitle = "File Manager"
    static let cancelActionTitle = "Cancel"
    static let mediaTypes = ["public.movie"]
    static let errorAlertTitle = "Error"
    static let lengthViolationAlertMessage = "Selected video exceeds 60 seconds."
    static let sizeViolationAlertMessage = "Selected video exceeds 200 MB."
    static let okMessage = "OK"
    static let videoMaxDuration = 60.0
    static let videoMaxFileSize = 200 * 1024 * 1024
  }
}

protocol VideoMetadataProvider {
  func duration(for url: URL) -> Double?
  func fileSize(for url: URL) -> UInt64?
}

struct DefaultVideoMetadataProvider: VideoMetadataProvider {
  func duration(for url: URL) -> Double? {
    let asset = AVAsset(url: url)
    return CMTimeGetSeconds(asset.duration)
  }

  func fileSize(for url: URL) -> UInt64? {
    do {
      let fileAttributes = try FileManager.default.attributesOfItem(atPath: url.path)
      return fileAttributes[.size] as? UInt64
    } catch {
      print("Error retrieving file size: \(error)")
      return nil
    }
  }
}
