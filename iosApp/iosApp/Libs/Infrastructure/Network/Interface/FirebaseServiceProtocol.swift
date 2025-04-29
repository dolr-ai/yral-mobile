//
//  FirebaseServiceProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

protocol FirebaseServiceProtocol {
  func fetchDocument<T: Decodable>(
    path: String,
    decodeAs type: T.Type
  ) async throws -> T

  func fetchCollection<T: Decodable>(
    from path: String,
    orderBy field: FieldPath?,
    decodeAs type: T.Type
  ) async throws -> [T]
}
