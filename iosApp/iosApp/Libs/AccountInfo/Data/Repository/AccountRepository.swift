//
//  ProfileRepository.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

class AccountRepository: AccountRepositoryProtocol {
  private let httpService: HTTPService
  private let authClient: AuthClient

  init(httpService: HTTPService, authClient: AuthClient) {
    self.httpService = httpService
    self.authClient = authClient
  }

  func fetchAccount() async -> Result<AccountInfo, AccountError> {
    var imageURL: URL?
    if let principalString = authClient.userPrincipalString,
       let principal = try? get_principal(principalString) {
      imageURL = URL(string: propic_from_principal(principal).toString())
    }
    return .success(AccountInfo(imageURL: imageURL, canisterID: authClient.userPrincipalString ?? ""))
  }
}
