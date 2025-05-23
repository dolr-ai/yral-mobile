//
//  HLSDownloadManager+Mockers.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 21/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import AVFoundation

protocol HLSDownloadManagerProtocol: AnyObject {
  func clearedCache(for assetTitle: String)
}

protocol AVAssetDownloadTaskProtocol: AnyObject {
  func resume()
  func cancel()
  var underlyingTask: AVAssetDownloadTask? { get }
}

protocol AVAssetDownloadURLSessionProtocol: AnyObject {
  func makeAssetDownloadTask(downloadConfiguration: AVAssetDownloadConfiguration) -> AVAssetDownloadTaskProtocol?
}

final class DefaultAssetDownloadTask: AVAssetDownloadTaskProtocol {
  private let realTask: AVAssetDownloadTask

  init(realTask: AVAssetDownloadTask) {
    self.realTask = realTask
  }

  func resume() {
    realTask.resume()
  }

  func cancel() {
    realTask.cancel()
  }

  var underlyingTask: AVAssetDownloadTask? {
    realTask
  }
}

final class DefaultAssetDownloadURLSession: AVAssetDownloadURLSessionProtocol {
  private let session: AVAssetDownloadURLSession

  init(session: AVAssetDownloadURLSession) {
    self.session = session
  }

  func makeAssetDownloadTask(downloadConfiguration: AVAssetDownloadConfiguration) -> AVAssetDownloadTaskProtocol? {
    let avTask = session.makeAssetDownloadTask(downloadConfiguration: downloadConfiguration)
    avTask.priority = URLSessionTask.lowPriority
    return DefaultAssetDownloadTask(realTask: avTask)
  }
}
