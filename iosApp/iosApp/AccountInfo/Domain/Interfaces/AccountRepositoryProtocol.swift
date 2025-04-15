//
//  ProfileRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol AccountRepositoryProtocol {
  func fetchProfile() async -> Result<AccountInfo, AccountError>
}
