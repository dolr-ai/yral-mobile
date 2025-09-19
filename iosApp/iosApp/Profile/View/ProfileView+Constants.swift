//
//  ProfileView+Constants.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

extension ProfileView {
  enum Constants {
    static let navigationTitle = "My Profile"
    static let navigationTitleFont = YralFont.pt20.bold.swiftUIFont
    static let navigationTitleTextColor = YralColor.grey50.swiftUIColor
    static let navigationTitlePadding = EdgeInsets(
      top: 20.0,
      leading: 0.0,
      bottom: 16.0,
      trailing: 0.0
    )

    static let vStackSpacing: CGFloat = 20.0
    static let horizontalPadding: CGFloat = 16.0
    static let minimumTopSpacing: CGFloat = 16.0
    static let minimumBottomSpacing: CGFloat = 16.0
    static let profileMenuImageSize = 32.0

    static let deleteTitle = "Delete video?"
    static let deleteText = "This video will be permanently deleted from your Yral account."
    static let cancelTitle = "Cancel"
    static let deleteButtonTitle = "Delete"
    static let profileMenuImageName = "profile_menu"
    static let bottomAdjustmentYralTabBat = 48.0
  }
}
