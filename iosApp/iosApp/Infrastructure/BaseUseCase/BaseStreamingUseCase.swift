//
//  BaseStreamingUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

open class BaseStreamingUseCase<Parameter, Success, DomainError: Error> {
  private let crashReporter: CrashReporter

  public init(crashReporter: CrashReporter) {
    self.crashReporter = crashReporter
  }

  open func execute(request: Parameter) -> AsyncThrowingStream<Success, Error> {
    let rawStream = runImplementation(request)
    return AsyncThrowingStream<Success, Error> { continuation in
      Task {
        do {
          for try await value in rawStream {
            continuation.yield(value)
          }
          continuation.finish()
        } catch {
          crashReporter.recordException(error)
          let mapped = convertToDomainError(error)
          continuation.finish(throwing: mapped)
        }
      }
    }
  }

  open func runImplementation(_ request: Parameter) -> AsyncThrowingStream<Success, Error> {
    fatalError("Subclasses must override runImplementation(_:).")
  }

  open func convertToDomainError(_ error: Error) -> DomainError {
    fatalError("Subclasses must override convertToDomainError(_:).")
  }
}
