//
//  ProfileRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

class ProfileRepository: ProfileRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchProfile() async -> Result<ProfileInfo, Error> {
    return .success(ProfileInfo(imageURL: nil, canisterID: authClient.principalString ?? ""))
  }
}
