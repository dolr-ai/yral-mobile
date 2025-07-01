//
//  ToastType.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

enum ToastType {
  case uploadSuccess
  case uploadFailure
  case reportSuccess

  var title: String {
    switch self {
    case .uploadSuccess:
      return Constants.uploadSuccessTitle
    case .uploadFailure:
      return Constants.failureTitle
    case .reportSuccess:
      return Constants.reportSuccessTitle
    }
  }

  var subtitle: String {
    switch self {
    case .uploadSuccess:
      return Constants.uploadSuccessSubtitle
    case .uploadFailure:
      return Constants.failureSubtitle
    case .reportSuccess:
      return Constants.reportSuccessSubtitle
    }
  }

  var subtitleColor: UIColor {
    switch self {
    case .uploadSuccess, .reportSuccess:
      return Constants.successSubtitleColor
    case .uploadFailure:
      return Constants.failureSubtitleColor
    }
  }

  var buttonTitle: String? {
    switch self {
    case .uploadSuccess, .reportSuccess:
      return nil
    case .uploadFailure:
      return Constants.failureButtonTitle
    }
  }

  var icon: UIImage? {
    switch self {
    case .uploadSuccess, .reportSuccess:
      return UIImage(named: Constants.successIconName)
    case .uploadFailure:
      return UIImage(named: Constants.failureIconName)
    }
  }

  var backgroundColor: UIColor {
    switch self {
    case .uploadSuccess, .uploadFailure, .reportSuccess:
      return Constants.bgColor
    }
  }
}

extension ToastType {
  enum Constants {
    static let uploadSuccessTitle = "Video Uploaded"
    static let uploadSuccessSubtitle = """
Your video has been uploaded successfully.
Browse and add more videos.
"""
    static let reportSuccessTitle = "Report Submitted!"
    static let reportSuccessSubtitle = "Thanks for letting us know! Our team will review this video soon."
    static let successSubtitleColor =  UIColor(red: 0.831, green: 0.831, blue: 0.831, alpha: 1)
    static let failureTitle = "Error"
    static let failureSubtitle = "Please try uploading your video again."
    static let failureSubtitleColor = UIColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1)
    static let failureButtonTitle = "Retry"
    static let successIconName = "toast_success"
    static let failureIconName = "toast_failure"
    static let bgColor = UIColor(red: 0.09, green: 0.09, blue: 0.09, alpha: 1)
  }
}
