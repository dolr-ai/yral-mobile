//
//  DeleteVideoUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import Combine

protocol DeleteVideoUseCaseProtocol {
  func execute(request: DeleteVideoRequest) async -> Result<Void, ProfileError>
  var deletedVideoPublisher: AnyPublisher<[FeedResult], Never> { get }
}

class DeleteVideoUseCase:
  BaseResultUseCase<DeleteVideoRequest, Void, ProfileError>,
  DeleteVideoUseCaseProtocol {
  let profileRepository: ProfileRepositoryProtocol
  var deletedVideoPublisher: AnyPublisher<[FeedResult], Never> {
    profileRepository.deletedVideoPublisher
  }

  init(profileRepository: ProfileRepositoryProtocol, crashReporter: CrashReporter) {
    self.profileRepository = profileRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: DeleteVideoRequest) async -> Result<Void, ProfileError> {
    await profileRepository.deleteVideo(request: request)
  }
}
