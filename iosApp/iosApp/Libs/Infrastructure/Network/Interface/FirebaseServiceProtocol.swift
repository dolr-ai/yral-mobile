//
//  FirebaseServiceProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

protocol FirebaseServiceProtocol {
  func signInAnonymously() async throws -> Bool
  func signIn(withCustomToken token: String) async throws
  func signOut() throws
  func fetchUserIDToken() async throws -> String?
  func fetchAppCheckToken() async throws -> String
  func fetchCoins(for principal: String) async throws -> Int

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
