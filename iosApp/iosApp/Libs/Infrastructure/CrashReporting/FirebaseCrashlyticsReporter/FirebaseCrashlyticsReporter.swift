//
//  FirebaseCrashlyticsReporter.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseCrashlytics

public final class FirebaseCrashlyticsReporter: CrashReporter {
  private let crashlytics = Crashlytics.crashlytics()

  public init() {}

  public func setUserId(_ userId: String) {
    crashlytics.setUserID(userId)
  }

  public func recordException(_ error: Error) {
    crashlytics.record(error: error)
  }

  public func log(_ message: String) {
    crashlytics.log(message)
  }

  public func setMetadata(key: String, value: String) {
    crashlytics.setCustomValue(value, forKey: key)
  }
}
