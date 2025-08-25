//
//  FirebaseServiceProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol FirebaseServiceProtocol {
  func signInAnonymously(with principal: String) async throws -> Bool
  func resetSession() async throws
  func signIn(withCustomToken token: String) async throws
  func signOut() throws
  func fetchUserIDToken() async throws -> String?
  func fetchAppCheckToken() async -> String?
  func fetchCoins(for principal: String) async throws -> Int
  func update(coins: UInt64, forPrincipal principal: String) async throws

  func fetchDocument<T: Decodable>(
    path: String,
    checkCache: Bool,
    decodeAs type: T.Type
  ) async throws -> T

  func fetchCollection<T: Decodable>(
    from path: String,
    orderBy fields: [String]?,
    descending: Bool,
    limit: Int?,
    decodeAs type: T.Type
  ) async throws -> [T]

  func documentExists(for path: String) async throws -> Bool
}
