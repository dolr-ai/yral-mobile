//
//  KeychainHelper.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Security

struct KeychainHelper {
  static func store(data: Data, for key: String) throws {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrAccount as String: key,
      kSecAttrService as String: Bundle.main.bundleIdentifier ?? "com.yral.ios",
      kSecValueData as String: data
    ]

    // Delete any existing items
    SecItemDelete(query as CFDictionary)

    // Add new item
    let status = SecItemAdd(query as CFDictionary, nil)
    guard status == errSecSuccess else {
      throw KeychainError.unhandledError(status: status)
    }
  }

  static func retrieveData(for key: String) throws -> Data? {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrAccount as String: key,
      kSecAttrService as String: Bundle.main.bundleIdentifier ?? "com.yral.ios",
      kSecReturnData as String: kCFBooleanTrue!,
      kSecMatchLimit as String: kSecMatchLimitOne
    ]

    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)

    guard status != errSecItemNotFound else { return nil }
    guard status == errSecSuccess else {
      throw KeychainError.unhandledError(status: status)
    }

    if let data = item as? Data {
      return data
    }
    return nil
  }

  static func deleteItem(for key: String) throws {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrAccount as String: key,
      kSecAttrService as String: Bundle.main.bundleIdentifier ?? "com.yral.ios"
    ]

    let status = SecItemDelete(query as CFDictionary)
    guard status == errSecSuccess || status == errSecItemNotFound else {
      throw KeychainError.unhandledError(status: status)
    }
  }
}

enum KeychainError: Error {
  case unhandledError(status: OSStatus)
}
