//
//  FirebaseService.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

class FirebaseService: FirebaseServiceProtocol {
  private let database = Firestore.firestore()

  func fetchDocument<T>(
    path: String,
    decodeAs type: T.Type
  ) async throws -> T where T: Decodable {
    let doc = database.document(path)
    let snapshot = try await doc.getDocument()

    return try snapshot.data(as: T.self)
  }

  func fetchCollection<T>(
    from path: String,
    orderBy field: FieldPath?,
    decodeAs type: T.Type
  ) async throws -> [T] where T: Decodable {
    var query: Query = database.collection(path)
    if let field {
      query = query.order(by: field)
    }

    let snapshot = try await query.getDocuments()
    return try snapshot.documents.compactMap { try $0.data(as: T.self) }
  }
}
