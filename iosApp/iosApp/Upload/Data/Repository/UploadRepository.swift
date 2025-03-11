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
        return .failure(.unknown(error))
      }
    }
  }

  func uploadVideoWithProgress(request: UploadVideoRequest) -> AsyncThrowingStream<Double, Error> {
    guard let url = URL(string: request.uploadURLString) else {
      return AsyncThrowingStream<Double, Error> { continuation in
        continuation.finish(
          throwing: VideoUploadError.invalidFileURL("Invalid URL: \(request.uploadURLString)")
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
      fileURL: request.fileURL,
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
            continuation.finish(throwing: VideoUploadError.unknown(error))
          }
        }
      }
    }
  }

  func updateMetadata(request: UploadVideoRequest) async -> Result<Void, VideoUploadError> {
    guard let baseURL = httpService.baseURL else {
      return .failure(.network(.invalidRequest))
    }
    do {
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let metaRequest = UpdateMetaDataRequest(
        videoUid: request.videoUID,
        delegatedIdentityWire: swiftWire,
        meta: [:],
        postDetails: PostDetailsFromFrontendRequest(
          hashtags: request.hashtags,
          description: request.caption,
          videoUid: request.videoUID
        )
      )
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.updateMetaDataPath,
        method: .post,
        headers: ["Content-Type": "application/json"],
        body: try JSONEncoder().encode(metaRequest)
      )
      _ = try await httpService.performRequest(for: endpoint)
      return .success(())
    } catch {
      switch error {
      case let netErr as NetworkError:
        return .failure(VideoUploadError.network(netErr))
      case let authErr as AuthError:
        return .failure(VideoUploadError.auth(authErr))
      default:
        return .failure(.unknown(error))
      }
    }
  }

  private func swiftDelegatedIdentityWire(from rustWire: DelegatedIdentityWire) throws -> SwiftDelegatedIdentityWire {
    let wireJsonString = delegated_identity_wire_to_json(rustWire).toString()
    guard let data = wireJsonString.data(using: .utf8) else {
      throw AuthError.authenticationFailed("Failed to convert wire JSON string to Data")
    }
    return try JSONDecoder().decode(SwiftDelegatedIdentityWire.self, from: data)
  }
}

extension UploadRepository {
  enum Constants {
    static let getVideoURLPath = "get_upload_url"
    static let updateMetaDataPath = "update_metadata"
  }
}
