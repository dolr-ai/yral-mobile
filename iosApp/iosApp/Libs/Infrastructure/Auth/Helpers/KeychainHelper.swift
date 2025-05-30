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
  private static let keychainQueue = DispatchQueue(label: "com.yral.keychain")

  static func store(data: Data, for key: String) throws {
    return try keychainQueue.sync {
      let query: [CFString: Any] = [
        kSecClass: kSecClassGenericPassword,
        kSecAttrAccount: key,
        kSecAttrService: Bundle.main.bundleIdentifier ?? "com.yral.ios",
        kSecValueData: data
      ]

      SecItemDelete(query as CFDictionary)

      let status = SecItemAdd(query as CFDictionary, nil)
      guard status == errSecSuccess else {
        throw KeychainError.unhandledError(status: status)
      }
    }
  }

  static func retrieveData(for key: String) throws -> Data? {
    return try keychainQueue.sync {
      let query: [CFString: Any] = [
        kSecClass: kSecClassGenericPassword,
        kSecAttrAccount: key,
        kSecAttrService: Bundle.main.bundleIdentifier ?? "com.yral.ios",
        kSecReturnData: kCFBooleanTrue!,
        kSecMatchLimit: kSecMatchLimitOne
      ]

      var item: CFTypeRef?
      let status = SecItemCopyMatching(query as CFDictionary, &item)

      guard status != errSecItemNotFound else {
        return nil
      }

      guard status == errSecSuccess else {
        throw KeychainError.unhandledError(status: status)
      }

      guard let data = item as? Data else {
        return nil
      }

      return data
    }
  }

  static func deleteItem(for key: String) throws {
    return try keychainQueue.sync {
      let query: [CFString: Any] = [
        kSecClass: kSecClassGenericPassword,
        kSecAttrAccount: key,
        kSecAttrService: Bundle.main.bundleIdentifier ?? "com.yral.ios"
      ]

      let status = SecItemDelete(query as CFDictionary)
      guard status == errSecSuccess || status == errSecItemNotFound else {
        throw KeychainError.unhandledError(status: status)
      }
    }
  }

  static func storeSet(_ set: Set<String>, for key: String) throws {
    let array = Array(set)
    let data = try JSONEncoder().encode(array)
    try store(data: data, for: key)
  }

  static func retrieveSet(for key: String) throws -> Set<String>? {
    guard let data = try retrieveData(for: key) else {
      return nil
    }
    let array = try JSONDecoder().decode([String].self, from: data)
    return Set(array)
  }

  static func store(_ value: Bool, for key: String) throws {
    try store(data: Data([value ? 1 : 0]), for: key)
  }

  static func retrieveBool(for key: String) throws -> Bool? {
    guard let data = try retrieveData(for: key),
          let byte  = data.first else { return nil }
    return byte == 1
  }

  static func bool(for key: String, default fallback: Bool) -> Bool {
    (try? retrieveBool(for: key))
      .flatMap { $0 }
    ?? fallback
  }

  static func store(_ string: String, for key: String) throws {
    let data = Data(string.utf8)
    try store(data: data, for: key)
  }

  static func retrieveString(for key: String) throws -> String? {
    guard let data = try retrieveData(for: key) else { return nil }
    return String(data: data, encoding: .utf8)
  }
}

enum KeychainError: Error {
  case unhandledError(status: OSStatus)
}

extension KeychainError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .unhandledError(let status):
      return "Unhandled keychain error with status: \(status)"
    }
  }
}
