//
//  HTTPService.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

class HTTPService: NetworkService {
  let baseURLString: String?
  var baseURL: URL? {
    guard let urlString = baseURLString else { return nil }
    return URL(string: urlString)
  }

  init(baseURLString: String? = nil) {
    self.baseURLString = baseURLString
  }

  func performRequest(for endPoint: Endpoint) async throws -> Data {
    guard endPoint.transport == .http,
          let baseURL = endPoint.baseURL ?? baseURL,
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

  func performMultipartRequestWithProgress(
    for endpoint: Endpoint,
    fileURL: URL,
    fileKey: String = "file",
    mimeType: String = "video/mp4"
  ) -> AsyncThrowingStream<Double, Error> {
    return AsyncThrowingStream { continuation in
      guard endpoint.transport == .http,
            let baseURL = endpoint.baseURL,
            let path = endpoint.path,
            let method = endpoint.httpMethod else {
        continuation.finish(throwing: NetworkError.invalidRequest)
        return
      }
      var urlComponents = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)
      urlComponents?.queryItems = endpoint.queryItems

      guard let finalURL = urlComponents?.url else {
        continuation.finish(throwing: NetworkError.invalidRequest)
        return
      }

      var request = URLRequest(url: finalURL)
      request.httpMethod = method.rawValue
      if let endpointHeaders = endpoint.headers {
        endpointHeaders.forEach { (key, val) in
          request.setValue(val, forHTTPHeaderField: key)
        }
      }

      let boundary = "Boundary-\(UUID().uuidString)"
      request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

      do {
        let fileData = try Data(contentsOf: fileURL)
        var body = Data()
        // swiftlint: disable all
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"\(fileKey)\"; filename=\"\(fileURL.lastPathComponent)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)
        // swiftlint: enable all

        let delegate = UploadTaskDelegate(continuation: continuation)
        let session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)

        let task = session.uploadTask(with: request, from: body)
        task.resume()
      } catch {
        continuation.finish(throwing: NetworkError.transportError("File read error: \(error.localizedDescription)"))
      }
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
