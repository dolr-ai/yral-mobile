//
//  ProfileRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol AccountRepositoryProtocol {
  func fetchAccount() async -> Result<AccountInfo, AccountError>
  func socialSignIn(provider: SocialProvider) async -> Result<Void, AccountError>
  func logout() async -> Result<Void, AccountError>
  func delete() async -> Result<Void, AccountError>
}
