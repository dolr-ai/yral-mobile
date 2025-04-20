//
//  LogUploadEventUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 14/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//
//
import Foundation

protocol LogUploadEventUseCaseProtocol {
  func execute(request: [VideoEventRequest]) async -> Result<Void, FeedError>
  func flushAllEvents() async
}

class LogUploadEventUseCase:
  BaseResultUseCase<[VideoEventRequest], Void, FeedError>,
  LogUploadEventUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol
  private var eventBuffer = [VideoEventRequest]()
  private var flushTimer: Timer?

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
    startPeriodicFlush()
  }

  deinit {
    flushTimer?.invalidate()
  }

  override func execute(request: [VideoEventRequest]) async -> Result<Void, FeedError> {
    eventBuffer.append(contentsOf: request)
    if eventBuffer.count >= Constants.bufferSize {
      return await flushEvents()
    }
    return .success(())
  }

  func flushAllEvents() async {
    _ = await flushEvents()
  }

  private func startPeriodicFlush() {
    flushTimer = Timer.scheduledTimer(withTimeInterval: Constants.timeDuration, repeats: true) { [weak self] _ in
      guard let self = self else { return }
      Task {
        _ = await self.flushEvents()
      }
    }
  }

  private func flushEvents() async -> Result<Void, FeedError> {
    guard !eventBuffer.isEmpty else { return .success(()) }
    let eventsToSend = eventBuffer
    eventBuffer.removeAll()

    let result = await super.execute(request: eventsToSend)
    switch result {
    case .success:
      return .success(())
    case .failure(let error):
      eventBuffer.insert(contentsOf: eventsToSend, at: .zero)
      return .failure(error)
    }
  }

  override func runImplementation(_ request: [VideoEventRequest]) async -> Result<Void, FeedError> {
    await feedRepository.logEvent(request: request)
  }
}

extension LogUploadEventUseCase {
  enum Constants {
    static let timeDuration = 30.0
    static let bufferSize = 10
  }
}
