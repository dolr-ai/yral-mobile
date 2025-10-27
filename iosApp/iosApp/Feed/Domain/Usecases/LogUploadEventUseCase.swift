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
  private let buffer = EventBuffer()
  private var flushTimer: DispatchSourceTimer?

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
    startPeriodicFlush()
  }

  deinit {
    flushTimer?.cancel()
  }

  override func execute(request: [VideoEventRequest]) async -> Result<Void, FeedError> {
    let shouldFlush = await buffer.append(request, threshold: Constants.bufferSize)
    if shouldFlush {
      return await flushEvents()
    }
    return .success(())
  }

  func flushAllEvents() async {
    _ = await flushEvents()
  }

  private func startPeriodicFlush() {
    let queue = DispatchQueue(label: "com.yral.logUploadFlush", qos: .background)
    let timer = DispatchSource.makeTimerSource(queue: queue)
    timer.schedule(
      deadline: .now() + Constants.timeDuration,
      repeating: Constants.timeDuration
    )
    timer.setEventHandler { [weak self] in
      Task { _ = await self?.flushEvents() }
    }
    timer.activate()
    self.flushTimer = timer
  }

  private func flushEvents() async -> Result<Void, FeedError> {
    let eventsToSend = await buffer.drain()
    guard !eventsToSend.isEmpty else { return .success(()) }
    let result = await super.execute(request: eventsToSend)
    if case .failure = result {
      await buffer.restore(eventsToSend)
    }
    return result
  }

  override func runImplementation(_ request: [VideoEventRequest]) async -> Result<Void, FeedError> {
    await feedRepository.logEvent(request: request)
  }
}

extension LogUploadEventUseCase {
  enum Constants {
    static let timeDuration = 10.0
    static let bufferSize = 10
  }
}
