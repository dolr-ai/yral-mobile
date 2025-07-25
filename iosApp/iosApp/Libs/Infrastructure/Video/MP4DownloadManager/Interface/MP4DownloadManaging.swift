//
//  MP4DownloadManaging.swift
//  iosApp
//
//  Created by Claude on 19/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import AVFoundation

protocol MP4DownloadManaging: Actor {
  func setDelegate(_ delegate: MP4DownloadManagerProtocol?)
  func startDownloadAsync(mp4URL: URL, assetTitle: String) async throws -> URL
  func prefetch(url: URL, assetTitle: String) async
  func createLocalAssetIfAvailable(for mp4URL: URL) -> AVURLAsset?
  func localOrInflightAsset(for mp4URL: URL) -> AVURLAsset?
  func cancelDownload(for mp4URL: URL)
  func elevatePriority(for url: URL)
  func clearMappingsAndCache(for mp4URL: URL, assetTitle: String)
}

protocol URLSessionDownloadTaskProtocol: AnyObject {
  func resume()
  func cancel()
  var underlyingTask: URLSessionDownloadTask? { get }
}

protocol URLSessionDownloadURLSessionProtocol: AnyObject {
  func makeDownloadTask(with url: URL) -> URLSessionDownloadTaskProtocol
}

final class DefaultURLSessionDownloadTask: URLSessionDownloadTaskProtocol {
  private let realTask: URLSessionDownloadTask
  init(realTask: URLSessionDownloadTask) { self.realTask = realTask }
  func resume() { realTask.resume() }
  func cancel() { realTask.cancel() }
  var underlyingTask: URLSessionDownloadTask? { realTask }
}

final class DefaultURLSessionDownloadURLSession: URLSessionDownloadURLSessionProtocol {
  private let session: URLSession
  init(session: URLSession) { self.session = session }
  func makeDownloadTask(with url: URL) -> URLSessionDownloadTaskProtocol {
    let task = session.downloadTask(with: url)
    task.priority = URLSessionTask.lowPriority
    return DefaultURLSessionDownloadTask(realTask: task)
  }
}
