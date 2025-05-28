//
//  NetworkService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

protocol NetworkService {
  @discardableResult func performRequest(for endPoint: Endpoint) async throws -> Data
  @discardableResult func performRequest<T: Decodable>(for endPoint: Endpoint, decodeAs type: T.Type) async throws -> T
  @discardableResult func performMultipartRequestWithProgress(
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
  case cloudFunctionError(CloudFunctionError)
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
    case .cloudFunctionError(let error):
      return "Cloud Function Error: \(error.localizedDescription)"
    }
  }
}

enum CloudFunctionError: Error {
  case alreadyVoted(CloudFunctionErrorType, String)
  case unknown(CloudFunctionErrorType, String)
}

extension CloudFunctionError: LocalizedError {
  public var errorDescription: String? {
    switch self {
    case .alreadyVoted(let code, let message):
      return "Already Voted Error: \(code), \(message)"
    case .unknown(let code, let message):
      return "Unknown Error: \(code), \(message)"
    }
  }
}

extension CloudFunctionError: CustomStringConvertible {
  public var description: String {
    switch self {
    case .alreadyVoted(_, let message):
      return message
    case .unknown(_, let message):
      return message
    }
  }
}

enum CloudFunctionErrorType: String, Decodable {
  case alreadyVoted = "DUPLICATE_VOTE"
  case unknown

  init(from decoder: Decoder) throws {
    let container = try decoder.singleValueContainer()
    let raw = try container.decode(String.self)
    self = CloudFunctionErrorType(rawValue: raw) ?? .unknown
  }
}

struct CloudFunctionErrorParentResponse: Decodable {
  let error: CloudFunctionErrorResponse
}

struct CloudFunctionErrorResponse: Decodable {
  let code: CloudFunctionErrorType
  let message: String
}
