//
//  ToggleLikeUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol ToggleLikeUseCaseProtocol: AnyObject {
  typealias PostID = Int
  func execute(request: LikeQuery) async -> Result<LikeResult, FeedError>
}

class ToggleLikeUseCase:
  BaseResultUseCase<LikeQuery, LikeResult, FeedError>,
  ToggleLikeUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: LikeQuery) async -> Result<LikeResult, FeedError> {
    await feedRepository.toggleLikeStatus(for: request)
  }
}
