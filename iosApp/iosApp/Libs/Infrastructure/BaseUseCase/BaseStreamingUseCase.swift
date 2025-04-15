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
          if let aggregated = error as? AggregatedError {
            for err in aggregated.errors {
              crashReporter.recordException(err)
              crashReporter.log(err.localizedDescription)
            }
          } else {
            crashReporter.recordException(error)
            crashReporter.log(error.localizedDescription)
          }
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
