//
//  HTTPService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

class HTTPService: NetworkService {
  func performRequest(for endPoint: Endpoint) async throws -> Data {
    guard endPoint.transport == .http,
          let baseURL = endPoint.baseURL,
          let path = endPoint.path,
          let method = endPoint.httpMethod else {
      throw NetworkError.invalidRequest
    }
    var urlComponents = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)
    urlComponents?.queryItems = endPoint.queryItems
    guard let finalURL = urlComponents?.url else {
      throw NetworkError.invalidRequest
    }

    var request = URLRequest(url: finalURL)
    request.httpMethod = method.rawValue
    request.allHTTPHeaderFields = endPoint.headers
    request.httpBody = endPoint.httpBody
    let (data, response) = try await URLSession.shared.data(for: request)
    guard let httpResponse = response as? HTTPURLResponse else {
      throw NetworkError.invalidResponse("")
    }
    guard HTTPResponse.from(statusCode: httpResponse.statusCode) == .success else {
      throw NetworkError.invalidResponse(httpResponse.description)
    }
    return data
  }

  func performRequest<T>(for endPoint: Endpoint, decodeAs type: T.Type) async throws -> T where T: Decodable {
    let data = try await performRequest(for: endPoint)
    do {
      let decoded = try JSONDecoder().decode(type, from: data)
      return decoded
    } catch {
      throw NetworkError.decodingFailed(error)
    }
  }
}

enum HTTPResponse {
  case success
  case clientError
  case serverError
  case informational
  case redirection
  case unknown

  static func from(statusCode: Int) -> HTTPResponse {
    switch statusCode {
    case 200...299:
      return .success
    case 400...499:
      return .clientError
    case 500...599:
      return .serverError
    case 100...199:
      return .informational
    case 300...399:
      return .redirection
    default:
      return .unknown
    }
  }
}
