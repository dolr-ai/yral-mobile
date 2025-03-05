//
//  UploadRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

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

  func uploadVideoWithProgress(
    fileURL: URL,
    uploadURLString: String
  ) -> AsyncThrowingStream<Double, Error> {

    guard let url = URL(string: uploadURLString) else {
      return AsyncThrowingStream<Double, Error> { continuation in
        continuation.finish(
          throwing: VideoUploadError.invalidFileURL("Invalid URL: \(uploadURLString)")
        )
      }
    }
    let endpoint = Endpoint(
      http: "videoUpload",
      baseURL: url,
      path: "",
      method: .post
    )

    let upstream = httpService.performMultipartRequestWithProgress(
      for: endpoint,
      fileURL: fileURL,
      fileKey: "file",
      mimeType: "video/mp4"
    )
    return AsyncThrowingStream<Double, Error> { continuation in
      Task {
        do {
          for try await progress in upstream {
            continuation.yield(progress)
          }
          continuation.finish()

        } catch {
          if let netErr = error as? NetworkError {
            continuation.finish(throwing: VideoUploadError.network(netErr))
          } else {
            continuation.finish(throwing: VideoUploadError.unknown)
          }
        }
      }
    }
  }
}

extension UploadRepository {
  enum Constants {
    static let getVideoURLPath = "get_upload_url"
  }
}
