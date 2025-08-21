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

  func getRateLimitStatus() async -> Result<RateLimitStatus, RateLimitStatusError> {
    guard let userPrincipalString = authClient.userPrincipalString else {
      return .failure(.auth(.authenticationFailed("No user principal found")))
    }

    do {
      let userPrincipal = try get_principal(userPrincipalString.intoRustString())
      let delegatedIdentity = try authClient.generateNewDelegatedIdentity()
      let status = try await get_rate_limit_status_core(
        userPrincipal,
        "VIDEOGEN",
        true,
        delegatedIdentity
      )

      return .success(status)
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

  func getGenerateVideoStatus(for counter: UInt64) async -> Result<String, GenerateVideoStatusError> {
    guard let userPrincipalString = authClient.userPrincipalString else {
      return .failure(.auth(.authenticationFailed("No user principal found")))
    }

    do {
      let userPrincipal = try get_principal(userPrincipalString.intoRustString())
      let delegatedIdentity = try authClient.generateNewDelegatedIdentity()
      let videoGenRequestKey = make_videogen_request_key(userPrincipal, counter)
      let rateLimitResult2 = try await poll_video_generation_status(delegatedIdentity, videoGenRequestKey)
      if let resultStatus = get_polling_result_status(rateLimitResult2) {
        let resultString = get_status_value(resultStatus)
        return .success(resultString.toString())
      } else {
        return .failure(.network(.invalidRequest))
      }
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

  func uploadVideo(with url: String) async -> Result<Void, UploadAIVideoError> {
    guard let baseURL = URL(string: AppConfiguration().anonIdentityBaseURLString) else {
      return .failure(.network(.invalidRequest))
    }

    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)

      let httpBody = UploadAIVideoRequest(
        videoURL: url,
        hashtags: [],
        description: "",
        isNSFW: false,
        enableHotOrNot: false,
        delegatedIdentityWire: swiftWire
      )

      let result = try await httpService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: baseURL,
          path: Constants.uploadVideoPath,
          method: .post,
          headers: ["Content-Type": "application/json"],
          body: try? JSONEncoder().encode(httpBody)
        )
      )

      return .success(())
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
    static let uploadVideoPath = "/api/upload_ai_video_from_url"
  }
}
