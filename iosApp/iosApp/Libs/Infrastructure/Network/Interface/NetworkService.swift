//
//  NetworkService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

protocol NetworkService {
  func performRequest(for endPoint: Endpoint) async throws -> Data
  func performRequest<T: Decodable>(for endPoint: Endpoint, decodeAs type: T.Type) async throws -> T
  func performMultipartRequestWithProgress(
    for endpoint: Endpoint,
    fileURL: URL,
    fileKey: String,
    mimeType: String
  ) -> AsyncThrowingStream<Double, Error>
}

enum NetworkError: Error {
  case invalidRequest
  case invalidResponse(String)
  case decodingFailed(Error)
  case transportError(String)
  case grpc(String)
}

extension NetworkError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .invalidRequest:
      return "Invalid Request"
    case .invalidResponse(let message):
      return "Invalid Response: \(message)"
    case .decodingFailed(let error):
      return "Decoding Failed: \(error.localizedDescription)"
    case .transportError(let message):
      return "Transport Error: \(message)"
    case .grpc(let message):
      return "gRPC Error: \(message)"
    }
  }
}
