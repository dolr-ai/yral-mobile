//
//  BaseUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

open class BaseResultUseCase<Parameter, Success, DomainError: Error> {
  private let crashReporter: CrashReporter

  public init(crashReporter: CrashReporter) {
    self.crashReporter = crashReporter
  }

  open func execute(request: Parameter) async -> Result<Success, DomainError> {
    let result = await runImplementation(request)

    switch result {
    case .success:
      return result
    case .failure(let error):
      if let aggregated = error as? AggregatedError {
        for err in aggregated.errors {
          crashReporter.recordException(err)
          crashReporter.log(err.localizedDescription)
        }
      } else {
        crashReporter.recordException(error)
        crashReporter.log(error.localizedDescription)
      }
      return result
    }
  }

  open func runImplementation(_ request: Parameter) async -> Result<Success, DomainError> {
    fatalError("Subclasses must override runImplementation(_:).")
  }
}
