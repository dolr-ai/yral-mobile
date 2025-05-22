//
//  PerformanceMonitor.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

public protocol PerformanceMonitor {
  func start()
  func stop()
  func incrementMetric(_ metric: String, by count: Int64)
  func setMetadata(key: String, value: String)
}

public final class CompositePerformanceMonitor: PerformanceMonitor {
  private let monitors: [PerformanceMonitor]

  public init(monitors: [PerformanceMonitor]) {
    self.monitors = monitors
  }

  public func start() {
    monitors.forEach { $0.start() }
  }

  public func stop() {
    monitors.forEach { $0.stop() }
  }

  public func incrementMetric(_ metric: String, by count: Int64) {
    monitors.forEach { $0.incrementMetric(metric, by: count) }
  }

  public func setMetadata(key: String, value: String) {
    monitors.forEach { $0.setMetadata(key: key, value: value) }
  }
}
