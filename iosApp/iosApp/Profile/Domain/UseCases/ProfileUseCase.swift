//
//  ProfileUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol ProfileUseCaseProtocol {
  func execute() async -> Result<ProfileInfo, Error>
}

class ProfileUseCase: ProfileUseCaseProtocol {
  let profileRepository: ProfileRepositoryProtocol

  init(profileRepository: ProfileRepositoryProtocol) {
    self.profileRepository = profileRepository
  }

  func execute() async -> Result<ProfileInfo, Error> {
    await profileRepository.fetchProfile()
  }
}
