//
//  UsernameGenerator.swift
//  iosApp
//
//  Created by Samarth Paboowal on 14/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import CryptoKit

final class UsernameGenerator {
  static let shared = UsernameGenerator()

  private let nouns: [String]
  private let adjectives: [String]
  private let randDigitCount = 3
  private let maxLength = 15

  private init() {
    nouns = NOUNS
    adjectives = ADJECTIVES
  }

  func generateUsername(from principal: String) -> String {
    let hash = SHA256.hash(data: Data(principal.utf8))
    let seed = [UInt8](hash.prefix(32))

    var rng = SeededGenerator(seed: seed)
    let noun = nouns.randomElement(using: &rng) ?? "apple"
    let adjective = adjectives.randomElement(using: &rng) ?? "bold"

    var base = noun + adjective
    if base.count > (maxLength - randDigitCount) {
      base = String(base.prefix(maxLength - randDigitCount))
    }

    for _ in 0..<randDigitCount {
      let digit = Int.random(in: 0...9, using: &rng)
      base.append(String(digit))
    }

    return base
  }
}
