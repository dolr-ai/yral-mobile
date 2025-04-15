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
  private let likeRepository: LikeRepositoryProtocol

  init(likeRepository: LikeRepositoryProtocol, crashReporter: CrashReporter) {
    self.likeRepository = likeRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: LikeQuery) async -> Result<LikeResult, FeedError> {
    await likeRepository.toggleLikeStatus(for: request)
  }
}
