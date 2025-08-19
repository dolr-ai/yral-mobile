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
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
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

  func generateVideo(for request: GenerateVideoMetaRequest) async -> Result<GenerateVideoResponse, GenerateVideoError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }

    guard let userPrincipal = authClient.userPrincipalString else {
      return .failure(.auth(.authenticationFailed("No user principal found")))
    }

    let httpBody = request.addingPrincipal(userPrincipal)

    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let modifiedHttpBody = httpBody.addingDelegatedIdentity(swiftWire)

      let response = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: Constants.generateVideoPath,
          method: .post,
          headers: ["Content-Type": "application/json"],
          body: try? JSONEncoder().encode(modifiedHttpBody)
        ),
        decodeAs: GenerateVideoDTO.self
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
  private func swiftDelegatedIdentityWire(from rustWire: DelegatedIdentityWire) throws -> SwiftDelegatedIdentityWire {
    let wireJsonString = delegated_identity_wire_to_json(rustWire).toString()
    guard let data = wireJsonString.data(using: .utf8) else {
      throw AuthError.authenticationFailed("Failed to convert wire JSON string to Data")
    }
    return try JSONDecoder().decode(SwiftDelegatedIdentityWire.self, from: data)
  }
}

extension AIVideoRepository {
  enum Constants {
    static let getProvidersPath = "/api/v2/videogen/providers"
    static let generateVideoPath = "/api/v2/videogen/generate"
  }
}
