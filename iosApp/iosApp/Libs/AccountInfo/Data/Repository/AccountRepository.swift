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
    do {
      guard let baseURL = httpService.baseURL else { return .failure(.invalidInfo("No base URL found")) }
      let delegatedWire = try authClient.generateNewDelegatedIdentityWireOneHour()
      let swiftWire = try swiftDelegatedIdentityWire(from: delegatedWire)
      let deleteAccountDTO = DeleteAccountDTO(delegatedIdentityWire: swiftWire)
      let endpoint = Endpoint(
        http: "",
        baseURL: baseURL,
        path: Constants.deleteAccountPath,
        method: .delete,
        headers: ["Content-Type": "application/json"],
        body: try JSONEncoder().encode(deleteAccountDTO)
      )
      _ = try await httpService.performRequest(for: endpoint)
      _ = await logout()
      return .success(())
    } catch {
      switch error {
      case let netErr as NetworkError:
        return .failure(AccountError.networkError(netErr.localizedDescription))
      case let authErr as AuthError:
        return .failure(AccountError.authError(authErr))
      default:
        return .failure(.unknown(error.localizedDescription))
      }
    }
  }

  private func swiftDelegatedIdentityWire(from rustWire: DelegatedIdentityWire) throws -> SwiftDelegatedIdentityWire {
    let wireJsonString = delegated_identity_wire_to_json(rustWire).toString()
    guard let data = wireJsonString.data(using: .utf8) else {
      throw AuthError.authenticationFailed("Failed to convert wire JSON string to Data")
    }
    return try JSONDecoder().decode(SwiftDelegatedIdentityWire.self, from: data)
  }
}

extension AccountRepository {
  enum Constants {
    static let offset: UInt64 = 10
    static let deleteAccountPath = "/api/v1/user"
  }
}
