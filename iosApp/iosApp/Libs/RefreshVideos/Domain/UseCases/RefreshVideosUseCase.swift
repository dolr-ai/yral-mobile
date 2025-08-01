//
//  RefreshVideosUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 21/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Combine

protocol RefreshVideosUseCaseProtocol {
  func execute(request: RefreshVideosRequest) async -> Result<[FeedResult], AccountError>
}

@MainActor class RefreshVideosUseCase:
  BaseResultUseCase<RefreshVideosRequest, [FeedResult], AccountError>,
  RefreshVideosUseCaseProtocol {
  let profileRepository: ProfileRepositoryProtocol
  init(profileRepository: ProfileRepositoryProtocol, crashReporter: CrashReporter) {
    self.profileRepository = profileRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: RefreshVideosRequest) async -> Result<[FeedResult], AccountError> {
    await profileRepository.refreshVideos(shouldPurge: request.shouldPurge)
  }
}
