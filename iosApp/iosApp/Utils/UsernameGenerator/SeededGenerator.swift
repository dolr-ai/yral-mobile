//
//  SeededGenerator.swift
//  iosApp
//
//  Created by Samarth Paboowal on 14/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SeededGenerator: RandomNumberGenerator {
  private var state: [UInt8]
  private var index = 0

  init(seed: [UInt8]) {
    self.state = seed.count >= 32 ? seed : seed + [UInt8](repeating: 0, count: 32 - seed.count)
  }

  mutating func next() -> UInt64 {
    if index + 8 > state.count {
      index = 0
    }
    let value = state[index..<index+8].reduce(0) { ($0 << 8) | UInt64($1) }
    index += 8
    return value
  }
}
