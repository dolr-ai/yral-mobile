//
//  AppCheckProviderFactory.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import FirebaseCore
import FirebaseAppCheck

class YralAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
  func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
    return AppAttestProvider(app: app)
  }
}
