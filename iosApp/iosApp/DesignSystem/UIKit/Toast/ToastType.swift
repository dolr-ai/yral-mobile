//
//  ToastType.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import UIKit

enum ToastType {
  case success
  case failure

  var title: String {
    switch self {
    case .success:
      return Constants.successTitle
    case .failure:
      return Constants.failureTitle
    }
  }

  var subtitle: String {
    switch self {
    case .success:
      return Constants.successSubtitle
    case .failure:
      return Constants.failureSubtitle
    }
  }

  var subtitleColor: UIColor {
    switch self {
    case .success:
      return Constants.successSubtitleColor
    case .failure:
      return Constants.failureSubtitleColor
    }
  }

  var buttonTitle: String? {
    switch self {
    case .success:
      return nil
    case .failure:
      return Constants.failureButtonTitle
    }
  }

  var icon: UIImage? {
    switch self {
    case .success:
      return UIImage(named: Constants.successIconName)
    case .failure:
      return UIImage(named: Constants.failureIconName)
    }
  }

  var backgroundColor: UIColor {
    switch self {
    case .success, .failure:
      return Constants.bgColor
    }
  }
}

extension ToastType {
  enum Constants {
    static let successTitle = "Video Uploaded"
    static let successSubtitle = "Your video has been uploaded successfully. Browse and add more videos."
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
