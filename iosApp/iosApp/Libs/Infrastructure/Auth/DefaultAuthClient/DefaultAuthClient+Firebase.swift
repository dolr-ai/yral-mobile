//
//  DefaultAuthClient+Firebase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 08/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension DefaultAuthClient {
  func exchangePrincipalID(type: DelegateIdentityType) async throws {
    do {
      try await recordThrowingOperation {
        let newSignIn = try? await firebaseService.signInAnonymously(with: userPrincipalString ?? "")

        let userIDToken = try? await firebaseService.fetchUserIDToken()
        guard let userIDToken else {
          Task { [weak self] in
            guard let self = self else { return }
            await self.setAnalyticsData()
          }
          return
        }
        var httpHeaders = [
          "Content-Type": "application/json",
          "Authorization": "Bearer \(userIDToken)"
        ]
        if let appcheckToken = await firebaseService.fetchAppCheckToken() {
          httpHeaders["X-Firebase-AppCheck"] = appcheckToken
        }

        let httpBody: [String: String] = [
          "principal_id": userPrincipalString ?? ""
        ]

        if userPrincipalString != nil, !(newSignIn ?? true) {
          try await getUserBalance(type: type)
        } else {
          try await firstTimeSignIn(type: type, httpHeaders: httpHeaders, httpBody: httpBody)
        }
        Task { [weak self] in
          guard let self = self else { return }
          await self.setAnalyticsData()
        }
      }
    } catch {
      await updateAuthState(for: type, withCoins: 0, isFetchingCoins: false)
      print(error)
    }
  }

  func getUserBalance(type: DelegateIdentityType) async throws {
    guard let principalID = userPrincipalString else {
      throw SatsCoinError.unknown("Failed to fetch princiapl ID")
    }

    do {
      let response = try await networkService.performRequest(
        for: Endpoint(
          http: "",
          baseURL: satsBaseURL,
          path: "v2/balance/\(principalID)",
          method: .get
        ),
        decodeAs: SatsCoinDTO.self
      ).toDomain()
      await updateAuthState(for: type, withCoins: UInt64(response.balance) ?? 0, isFetchingCoins: false)
      try await firebaseService.update(coins: UInt64(response.balance) ?? 0, forPrincipal: principalID)
    } catch {
      switch error {
      case let error as NetworkError:
        throw SatsCoinError.network(error)
      default:
        throw SatsCoinError.unknown(error.localizedDescription)
      }
    }
  }

  private func firstTimeSignIn(
    type: DelegateIdentityType,
    httpHeaders: [String: String],
    httpBody: [String: String]
  ) async throws {
    let endpoint = Endpoint(
      http: "",
      baseURL: firebaseBaseURL,
      path: "exchange_principal_id",
      method: .post,
      headers: httpHeaders,
      body: try? JSONSerialization.data(withJSONObject: httpBody)
    )
    let response = try await networkService.performRequest(
      for: endpoint,
      decodeAs: ExchangePrincipalDTO.self
    ).toDomain()

    var retryCount = Int.three
    while retryCount > .zero {
      do {
        try await recordThrowingOperation {
          try await firebaseService.signIn(withCustomToken: response.token)
          retryCount = .zero
        }
      } catch {
        retryCount -= 1
        if retryCount == .zero {
          try await firebaseService.resetSession()
        }
      }
    }
    try await getUserBalance(type: type)
  }

  func updateAuthState(for type: DelegateIdentityType, withCoins coins: UInt64, isFetchingCoins: Bool) async {
    await MainActor.run {
      stateSubject.value = (type == .ephemeral) ? .ephemeralAuthentication(
        userPrincipal: userPrincipalString ?? "",
        canisterPrincipal: canisterPrincipalString ?? "",
        coins: coins,
        isFetchingCoins: isFetchingCoins
      ) : .permanentAuthentication(
        userPrincipal: userPrincipalString ?? "",
        canisterPrincipal: canisterPrincipalString ?? "",
        coins: coins,
        isFetchingCoins: isFetchingCoins
      )
    }
  }
}
