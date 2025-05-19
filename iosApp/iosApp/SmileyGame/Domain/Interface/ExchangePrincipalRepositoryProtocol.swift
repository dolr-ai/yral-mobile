//
//  ExchangePrincipalRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ExchangePrincipalRepositoryProtocol {
  func exchangePrincipal() async -> Result<ExchangePrincipalResponse, ExchangePrincipalError>
}
