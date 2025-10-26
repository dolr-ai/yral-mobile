//
//  UserDefaultsManager.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

enum DefaultsKey: String {
  case hasLaunchedAppBefore
  case eulaAccepted
  case userDefaultsLoggedIn
  case authIdentityExpiryDateKey
  case authRefreshTokenExpiryDateKey
  case keychainMigrationDone
  case onboardingCompleted
  case isServiceCanisterUser
  case username
}

final class UserDefaultsManager {
  static let shared = UserDefaultsManager()
  private let defaults = UserDefaults.standard

  func set<T>(_ value: T, for key: DefaultsKey) {
    defaults.set(value, forKey: key.rawValue)
  }

  func get<T>(for key: DefaultsKey) -> T? {
    return defaults.value(forKey: key.rawValue) as? T
  }

  func setCodable<T: Codable>(_ object: T, for key: DefaultsKey) {
    let encoder = JSONEncoder()
    if let data = try? encoder.encode(object) {
      defaults.set(data, forKey: key.rawValue)
    }
  }

  func getCodable<T: Codable>(_ type: T.Type, for key: DefaultsKey) -> T? {
    guard let data = defaults.data(forKey: key.rawValue) else { return nil }
    let decoder = JSONDecoder()
    return try? decoder.decode(type, from: data)
  }

  func remove(_ key: DefaultsKey) {
    defaults.removeObject(forKey: key.rawValue)
  }
}
