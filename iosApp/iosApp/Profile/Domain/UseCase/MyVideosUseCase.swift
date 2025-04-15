//
//  MyVideosUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 19/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine

protocol MyVideosUseCaseProtocol {
  func execute(request: ProfileVideoRequest) async -> Result<[FeedResult], AccountError>
  var videosPublisher: AnyPublisher<[FeedResult], Never> { get }
  var newVideosPublisher: AnyPublisher<[FeedResult], Never> { get }
}

class MyVideosUseCase:
  BaseResultUseCase<ProfileVideoRequest, [FeedResult], AccountError>,
  MyVideosUseCaseProtocol {
  let profileRepository: ProfileRepositoryProtocol
  var videosPublisher: AnyPublisher<[FeedResult], Never> {
    profileRepository.videosPublisher
  }

  var newVideosPublisher: AnyPublisher<[FeedResult], Never> {
    profileRepository.newVideosPublisher
  }

  init(profileRepository: ProfileRepositoryProtocol, crashReporter: CrashReporter) {
    self.profileRepository = profileRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: ProfileVideoRequest) async -> Result<[FeedResult], AccountError> {
    await profileRepository.fetchVideos(request: request)
  }
}
