//
//  CrashReporter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

public protocol CrashReporter {
  /// Sets the user identifier for debugging / crash correlation.
  func setUserId(_ userId: String)

  /// Records a non-fatal error/exception.
  func recordException(_ error: Error)

  /// Logs an informational message (helpful to see chronological logs in Crashlytics).
  func log(_ message: String)

  /// Attaches any custom metadata (key-value pair).
  func setMetadata(key: String, value: String)
}

public final class CompositeCrashReporter: CrashReporter {
  private let reporters: [CrashReporter]

  public init(reporters: [CrashReporter]) {
    self.reporters = reporters
  }

  public func setUserId(_ userId: String) {
    reporters.forEach { $0.setUserId(userId) }
  }

  public func recordException(_ error: Error) {
    reporters.forEach { $0.recordException(error) }
  }

  public func log(_ message: String) {
    reporters.forEach { $0.log(message) }
  }

  public func setMetadata(key: String, value: String) {
    reporters.forEach { $0.setMetadata(key: key, value: value) }
  }
}
