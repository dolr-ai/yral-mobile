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

  func socialSignIn(provider: SocialProvider) async -> Result<Void, AccountError> {
    do {
      try await authClient.signInWithSocial(provider: provider)
      return .success(())
    } catch {
      if let error = error as? AuthError {
        return .failure(AccountError.authError(error))
      }
      return .failure(AccountError.unknown(error.localizedDescription))
    }
  }

  func logout() async -> Result<Void, AccountError> {
    do {
      try await authClient.logout()
      return .success(())
    } catch {
      if let error = error as? AuthError {
        return .failure(AccountError.authError(error))
      }
      return .failure(AccountError.unknown(error.localizedDescription))
    }
  }

  func delete() async -> Result<Void, AccountError> {
    var videoIdsToRemove: [String] = []
    let result = await getUserVideoIds(videoIdsToRemove: &videoIdsToRemove)
    _ = await logout()
    switch result {
    case .success(let videoIdsToRemove):
      do {
        try KeychainHelper.storeSet(Set(videoIdsToRemove), for: DefaultAuthClient.Constants.keychainDeletedVideosKey)
//        try KeychainHelper.deleteItem(for: DefaultAuthClient.Constants.keychainDeletedVideosKey)
      } catch {
        return .failure(AccountError.keychainError(error.localizedDescription))
      }
      return .success(())
    case .failure(let failure):
      return .failure(failure)
    }
  }

  private func getUserVideoIds(
    startIndex: UInt64 = .zero,
    offset: UInt64 = Constants.offset,
    videoIdsToRemove: inout [String]
  ) async -> Result<[String], AccountError> {
    var startIndex = startIndex

    guard let principalString = authClient.canisterPrincipalString else {
      return .failure(AccountError.authError(AuthError.invalidRequest("No canister principal")))
    }
    do {
      let identity = try self.authClient.generateNewDelegatedIdentity()
      let principal = try get_principal(principalString)
      let service = try Service(principal, identity)
      let result = try await service.get_posts_of_this_user_profile_with_pagination_cursor(
        startIndex,
        offset
      )
      if result.is_ok() {
        guard let postResult = result.ok_value() else { return .success([String]()) }
        videoIdsToRemove += postResult.map { postDetail in
          postDetail.video_uid().toString()
        }
        startIndex += offset
        return await getUserVideoIds(startIndex: startIndex, offset: offset, videoIdsToRemove: &videoIdsToRemove)
      } else {
        guard let error = result.err_value() else { return .failure(AccountError.unknown("Invalid state")) }
        if error.is_reached_end_of_items_list() {
          return .success(videoIdsToRemove)
        } else {
          return .failure(AccountError.unknown("Invalid state"))
        }
      }
    } catch {
      return .failure(AccountError.rustError(RustError.unknown(error.localizedDescription)))
    }
  }
}

extension AccountRepository {
  enum Constants {
    static let offset: UInt64 = 10
  }
}
