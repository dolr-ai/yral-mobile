//
//  ProfileUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ProfileUseCaseProtocol {
  func execute() async -> Result<ProfileInfo, ProfileError>
}

class ProfileUseCase:
  BaseResultUseCase<Void, ProfileInfo, ProfileError>,
  ProfileUseCaseProtocol {
  let profileRepository: ProfileRepositoryProtocol

  init(profileRepository: ProfileRepositoryProtocol, crashReporter: CrashReporter) {
    self.profileRepository = profileRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<ProfileInfo, ProfileError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<ProfileInfo, ProfileError> {
    await profileRepository.fetchProfile()
  }
}
