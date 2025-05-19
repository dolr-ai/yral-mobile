//
//  ExchangePrincipalRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class ExchangePrincipalRepository: ExchangePrincipalRepositoryProtocol {
  private let firebaseService: FirebaseService
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(firebaseService: FirebaseService, httpService: HTTPService, authClient: AuthClient) {
    self.firebaseService = firebaseService
    self.httpService = httpService
    self.authClient = authClient
  }

  func exchangePrincipal() async -> Result<ExchangePrincipalResponse, ExchangePrincipalError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }

    var httpHeaders = [
      "Content-Type": "application/json"
    ]

    do {
      guard let userIDToken = try await firebaseService.fetchUserIDToken() else {
        return .failure(.unknown("Failed to fetch user ID token"))
      }
      httpHeaders["Authorization"] = "Bearer \(userIDToken)"
    } catch {
      return .failure(.firebaseError(error))
    }

    let httpBody: [String: String] = [
      "principal_id": authClient.userPrincipalString ?? ""
    ]

    do {
      let response = try await httpService.performRequest(
        for: Endpoint(http: "",
                      baseURL: baseURL,
                      path: "exchange_principal_id",
                      method: .post,
                      headers: httpHeaders,
                      body: try? JSONSerialization.data(withJSONObject: httpBody)
                     ),
        decodeAs: ExchangePrincipalDTO.self
      )
      return .success(response.toDomain())
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(.network(error))
      default:
        return .failure(.firebaseError(error))
      }
    }
  }
}
