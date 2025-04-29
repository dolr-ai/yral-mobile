//
//  PKCE.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import CryptoKit

struct PKCE {
  static func generateCodeVerifier() -> String {
    let data = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    return data.base64URLEncodedString()
      .replacingOccurrences(of: "+", with: "-")
      .replacingOccurrences(of: "/", with: "_")
      .replacingOccurrences(of: "=", with: "")
  }

  static func codeChallenge(for verifier: String) -> String {
    let digest = SHA256.hash(data: Data(verifier.utf8))
    return Data(digest).base64URLEncodedString()
      .replacingOccurrences(of: "+", with: "-")
      .replacingOccurrences(of: "/", with: "_")
      .replacingOccurrences(of: "=", with: "")
  }
}
