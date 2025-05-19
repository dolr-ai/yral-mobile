//
//  FirebaseService.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore
import FirebaseAuth
import FirebaseAppCheck

class FirebaseService: FirebaseServiceProtocol {
  private let database = Firestore.firestore()

  func fetchUserIDToken() async throws -> String? {
    do {
      let idToken = try await Auth.auth().currentUser?.getIDToken()
      return idToken
    } catch {
      throw(error)
    }
  }

  func fetchAppCheckToken() async throws -> String {
    do {
      let appCheckToken = try await AppCheck.appCheck().token(forcingRefresh: false).token
      return appCheckToken
    } catch {
      throw(error)
    }
  }

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
    orderBy fields: [String]?,
    decodeAs type: T.Type
  ) async throws -> [T] where T: Decodable {
    var query: Query = database.collection(path)
    if let fields {
      query = query.order(by: FieldPath(fields))
    }

    let snapshot = try await query.getDocuments()
    return try snapshot.documents.compactMap { try $0.data(as: T.self) }
  }
}
