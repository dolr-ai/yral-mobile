//
//  AccountDetailsUtility.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import iosSharedUmbrella

class AccountDetailsUtility {
  static let shared = AccountDetailsUtility()

  func getDetails() -> AccountLinksDto? {
    return AppDIHelper().getFeatureFlagManager().get(
      flag: AccountFeatureFlags.AccountLinks.shared.Links
    ) as? AccountLinksDto
  }
}
