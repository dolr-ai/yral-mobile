//
//  MediaPickerTests.swift
//  YralTests
//
//  Created by Sarvesh Sharma on 16/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import XCTest
import AVFoundation
import UniformTypeIdentifiers
@testable import Yral

class VideoPickerViewControllerTests: XCTestCase {

  var viewController: TestableVideoPickerViewController!
  var fakeMetadataProvider: FakeVideoMetadataProvider!

  override func setUp() {
    super.setUp()
    viewController = TestableVideoPickerViewController()
    fakeMetadataProvider = FakeVideoMetadataProvider()
    viewController.videoMetadataProvider = fakeMetadataProvider
    _ = viewController.view
  }

  func testShowVideoPickerOptions() {
    viewController.showVideoPickerOptions()

    guard let alert = viewController.presentedVC as? UIAlertController else {
      XCTFail("Expected a UIAlertController to be presented.")
      return
    }
    XCTAssertEqual(alert.title, VideoPickerViewController.Constants.pickerOptionsTitle)

    let actionTitles = alert.actions.compactMap { $0.title }
    if UIImagePickerController.isSourceTypeAvailable(.camera) {
      XCTAssertTrue(actionTitles.contains(VideoPickerViewController.Constants.cameraActionTitle))
    }
    if UIImagePickerController.isSourceTypeAvailable(.photoLibrary) {
      XCTAssertTrue(actionTitles.contains(VideoPickerViewController.Constants.photoActionTitle))
    }
    XCTAssertTrue(actionTitles.contains(VideoPickerViewController.Constants.fileManagerActionTitle))
    XCTAssertTrue(actionTitles.contains(VideoPickerViewController.Constants.cancelActionTitle))
  }

  func testOpenCamera() {
    viewController.openCamera()
    guard let picker = viewController.presentedVC as? UIImagePickerController else {
      XCTFail("Expected a UIImagePickerController to be presented.")
      return
    }
    XCTAssertEqual(picker.sourceType, .camera)
    XCTAssertEqual(picker.mediaTypes, VideoPickerViewController.Constants.mediaTypes)
    XCTAssertEqual(picker.videoMaximumDuration, VideoPickerViewController.Constants.videoMaxDuration)
  }

  func testOpenPhotoLibrary() {
    viewController.openPhotoLibrary()
    guard let picker = viewController.presentedVC as? UIImagePickerController else {
      XCTFail("Expected a UIImagePickerController to be presented.")
      return
    }
    XCTAssertEqual(picker.sourceType, .photoLibrary)
    XCTAssertEqual(picker.mediaTypes, VideoPickerViewController.Constants.mediaTypes)
  }

  func testOpenDocumentPicker() {
    viewController.openDocumentPicker()

    guard let documentPicker = viewController.presentedVC as? UIDocumentPickerViewController else {
      XCTFail("Expected a UIDocumentPickerViewController to be presented.")
      return
    }
  }

  func testValidateVideo_validVideo() {
    fakeMetadataProvider.fakeDuration = 50.0
    fakeMetadataProvider.fakeFileSize = 150 * 1024 * 1024

    let dummyURL = URL(string: "file://dummy.mp4")!
    let isValid = viewController.exposedValidateVideo(url: dummyURL)
    XCTAssertTrue(isValid)
    XCTAssertNil(viewController.lastAlertTitle)
    XCTAssertNil(viewController.lastAlertMessage)
  }

  func testValidateVideo_durationExceedsLimit() {
    fakeMetadataProvider.fakeDuration = 70.0
    fakeMetadataProvider.fakeFileSize = 150 * 1024 * 1024

    let dummyURL = URL(string: "file://dummy.mp4")!
    let isValid = viewController.exposedValidateVideo(url: dummyURL)
    XCTAssertFalse(isValid)
    XCTAssertEqual(viewController.lastAlertTitle, VideoPickerViewController.Constants.errorAlertTitle)
    XCTAssertEqual(viewController.lastAlertMessage, VideoPickerViewController.Constants.lengthViolationAlertMessage)
  }

  func testValidateVideo_fileSizeExceedsLimit() {
    fakeMetadataProvider.fakeDuration = 50.0
    fakeMetadataProvider.fakeFileSize = 250 * 1024 * 1024

    let dummyURL = URL(string: "file://dummy.mp4")!
    let isValid = viewController.exposedValidateVideo(url: dummyURL)
    XCTAssertFalse(isValid)
    XCTAssertEqual(viewController.lastAlertTitle, VideoPickerViewController.Constants.errorAlertTitle)
    XCTAssertEqual(viewController.lastAlertMessage, VideoPickerViewController.Constants.sizeViolationAlertMessage)
  }
}

class FakeVideoMetadataProvider: VideoMetadataProvider {
  var fakeDuration: Double?
  var fakeFileSize: UInt64?

  func duration(for url: URL) -> Double? {
    return fakeDuration
  }

  func fileSize(for url: URL) -> UInt64? {
    return fakeFileSize
  }
}

class TestableVideoPickerViewController: VideoPickerViewController {
  var presentedVC: UIViewController?
  var lastAlertTitle: String?
  var lastAlertMessage: String?

  override func present(_ viewControllerToPresent: UIViewController,
                        animated flag: Bool,
                        completion: (() -> Void)? = nil) {
    presentedVC = viewControllerToPresent
    if let alert = viewControllerToPresent as? UIAlertController,
       alert.preferredStyle == .alert {
      lastAlertTitle = alert.title
      lastAlertMessage = alert.message
    }
    completion?()
  }

  func exposedValidateVideo(url: URL) -> Bool {
    return validateVideo(url: url)
  }
}
