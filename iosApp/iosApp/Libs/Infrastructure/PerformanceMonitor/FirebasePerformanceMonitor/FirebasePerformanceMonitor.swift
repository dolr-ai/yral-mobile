//
//  FirebasePerformanceMonitor.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 20/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebasePerformance

public final class FirebasePerformanceMonitor: PerformanceMonitor {
  private let trace: Trace?

  public init(traceName: String) {
    self.trace = Performance().trace(name: traceName)
  }

  public func start() {
    self.trace?.start()
  }

  public func stop() {
    self.trace?.stop()
  }

  public func incrementMetric(_ metric: String, by count: Int64) {
    self.trace?.incrementMetric(metric, by: count)
  }

  public func setMetadata(key: String, value: String) {
    self.trace?.setValue(value, forAttribute: key)
  }
}
