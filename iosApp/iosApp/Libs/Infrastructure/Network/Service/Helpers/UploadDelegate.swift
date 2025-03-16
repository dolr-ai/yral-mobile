//
//  UploadDelegate.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

class UploadTaskDelegate: NSObject, URLSessionTaskDelegate, URLSessionDataDelegate {
  let continuation: AsyncThrowingStream<Double, Error>.Continuation
  private var responseData = Data()

  init(continuation: AsyncThrowingStream<Double, Error>.Continuation) {
    self.continuation = continuation
  }

  func urlSession(
    _ session: URLSession,
    task: URLSessionTask,
    didSendBodyData bytesSent: Int64,
    totalBytesSent: Int64,
    totalBytesExpectedToSend: Int64
  ) {
    guard totalBytesExpectedToSend > .zero else { return }
    let fraction = Double(totalBytesSent) / Double(totalBytesExpectedToSend)
    continuation.yield(fraction)
  }

  func urlSession(_ session: URLSession,
                  dataTask: URLSessionDataTask,
                  didReceive data: Data) {
    responseData.append(data)
  }

  func urlSession(
    _ session: URLSession,
    task: URLSessionTask,
    didCompleteWithError error: Error?
  ) {
    if let error = error {
      let wrapped = NetworkError.transportError(
        "Upload error: \(error.localizedDescription)")
      continuation.finish(throwing: wrapped)
    } else {
      if let httpResponse = task.response as? HTTPURLResponse {
        let statusCode = httpResponse.statusCode
        print("Upload Status Code = \(statusCode)")
        print("Upload Headers = \(httpResponse.allHeaderFields)")
        let bodyString = String(data: responseData, encoding: .utf8) ?? "<nil>"
        print("Upload body: \(bodyString)")
      }
      continuation.finish()
    }
  }
}
