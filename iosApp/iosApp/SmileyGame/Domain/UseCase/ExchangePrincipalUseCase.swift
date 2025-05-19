//
//  ExchangePrincipalUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ExchangePrincipalUseCaseProtocol {
  func execute() async -> Result<ExchangePrincipalResponse, ExchangePrincipalError>
}

class ExchangePrincipalUseCase: BaseResultUseCase<Void, ExchangePrincipalResponse, ExchangePrincipalError>,
                                ExchangePrincipalUseCaseProtocol {
  private let exchangePrincipalRepository: ExchangePrincipalRepositoryProtocol

  init(exchangePrincipalRepository: ExchangePrincipalRepositoryProtocol, crashReporter: CrashReporter) {
    self.exchangePrincipalRepository = exchangePrincipalRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<ExchangePrincipalResponse, ExchangePrincipalError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<ExchangePrincipalResponse, ExchangePrincipalError> {
    await exchangePrincipalRepository.exchangePrincipal()
  }
}
