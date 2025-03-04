//
//  UploadRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

class UploadRepository: UploadRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchUploadUrl() async -> Result<UploadEndpointResponse, VideoUploadError> {
    guard let baseURL = httpService.baseURL else { return .failure(.network(.invalidRequest)) }
    do {
      let response = try await httpService.performRequest(
        for: Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.getVideoURLPath,
        method: .get
        ),
        decodeAs: UploadResponseDTO.self
      )
      return .success(response.toDomain())
    } catch {
      switch error {
      case let error as NetworkError:
        return .failure(.network(error))
      default:
        return .failure(.unknown)
      }
    }
  }
}

extension UploadRepository {
  enum Constants {
    static let getVideoURLPath = "get_upload_url"
  }
}
