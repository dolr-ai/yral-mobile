//
//  AIVideoRepository.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

class AIVideoRepository: AIVideoRepositoryProtocol {
  private let httpService: HTTPService

  init(httpService: HTTPService) {
    self.httpService = httpService
  }

  func getProviders() async -> Result<AIVideoProviderMetaResponse, AIVideoProviderError> {
    guard let baseURL = httpService.baseURL else { return .failure(.network(.invalidRequest)) }
    do {
      let response = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: Constants.getProvidersPath,
          method: .get
        ),
        decodeAs: AIVideoProviderMetaDTO.self
      )
      return .success(response.toDomain())
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(.network(error))
      case let error as AuthError:
        return .failure(.auth(error))
      default:
        return .failure(.unknown(error))
      }
    }
  }
}

extension AIVideoRepository {
  enum Constants {
    static let getProvidersPath = "/api/v2/videogen/providers"
  }
}
