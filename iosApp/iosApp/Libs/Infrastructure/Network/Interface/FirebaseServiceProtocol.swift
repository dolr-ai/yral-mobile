//
//  FirebaseServiceProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

protocol FirebaseServiceProtocol {
  func signInAnonymously() async throws
  func signIn(withCustomToken token: String) async throws
  func fetchUserIDToken() async throws -> String?
  func fetchAppCheckToken() async throws -> String

  func fetchDocument<T: Decodable>(
    path: String,
    decodeAs type: T.Type
  ) async throws -> T

  func fetchCollection<T: Decodable>(
    from path: String,
    orderBy fields: [String]?,
    decodeAs type: T.Type
  ) async throws -> [T]
}
