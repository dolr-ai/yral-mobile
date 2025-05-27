//
//  EventBuffer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 26/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

actor EventBuffer {
  private var buffer: [VideoEventRequest] = []

  func append(_ events: [VideoEventRequest], threshold: Int) -> Bool {
    buffer.append(contentsOf: events)
    return buffer.count >= threshold
  }

  func drain() -> [VideoEventRequest] {
    let all = buffer
    buffer.removeAll()
    return all
  }

  func restore(_ failed: [VideoEventRequest]) {
    buffer.insert(contentsOf: failed, at: 0)
  }
}
