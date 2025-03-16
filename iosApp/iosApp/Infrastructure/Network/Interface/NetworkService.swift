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
