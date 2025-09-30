//
//  WalletRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine

protocol WalletRepositoryProtocol {
  func fetchSatoshiBalance() async -> Result<UInt32, WalletError>
  func fetchBTCExchangeRate() async -> Result<Double, WalletError>
}

class WalletRepository: WalletRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient
  private let firebaseService: FirebaseService
  init(
    httpService: HTTPService,
    authClient: AuthClient,
    firebaseService: FirebaseService
  ) {
    self.httpService = httpService
    self.authClient = authClient
    self.firebaseService = firebaseService
  }

  func fetchSatoshiBalance() async -> Result<UInt32, WalletError> {
    guard let principal = authClient.userPrincipalString,
          let service = LedgerService(principal)
    else { return .failure(WalletError.rustError(RustError.unknown("Ledger service couldn't initialize")))}
    guard let account = Account(principal)
    else { return .failure(WalletError.rustError(RustError.unknown("Ledger account couldn't initialize"))) }
    do {
      let balance = try await service.icrc_1_balance_of(account)
      return .success(balance)
    } catch {
      return .failure(WalletError.rustError(RustError.unknown(error.localizedDescription)))
    }
  }

  func fetchBTCExchangeRate() async -> Result<Double, WalletError> {
    guard let baseURL = httpService.baseURL
    else { return .failure(WalletError.networkError(NetworkError.invalidRequest))}
    do {
      var httpHeaders = [String: String]()
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(WalletError.firebaseError("Failed to fetch user ID token"))
      }
      httpHeaders = [
        "Content-Type": "application/json",
        "Authorization": "Bearer \(userIDToken)"
      ]
      if let appcheckToken = await firebaseService.fetchAppCheckToken() {
        httpHeaders["X-Firebase-AppCheck"] = appcheckToken
      }
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.exchangeRatePath,
        method: .get,
        queryItems: [URLQueryItem(name: "country_code", value: Locale.current.regionCode ?? "US")],
        headers: httpHeaders
      )
      let response = try await httpService.performRequest(for: endpoint, decodeAs: BtcPriceResponseDto.self)
      return .success(response.conversionRate)
    } catch {
      return .failure(WalletError.networkError(NetworkError.invalidResponse(error.localizedDescription)))
    }
  }
}

extension WalletRepository {
  enum Constants {
    static let exchangeRatePath = "btc_value_by_country"
  }
}
