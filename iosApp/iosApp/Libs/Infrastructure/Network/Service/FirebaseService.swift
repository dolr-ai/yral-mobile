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

  func signInAnonymously(with principal: String) async throws -> Bool {
    if let hasLaunchedAppBefore = (UserDefaultsManager.shared.get(for: DefaultsKey.hasLaunchedAppBefore) ?? false),
       !hasLaunchedAppBefore {
      try signOut()
      UserDefaultsManager.shared.set(true, for: DefaultsKey.hasLaunchedAppBefore)
    }

    if Auth.auth().currentUser?.uid != principal {
      try await Auth.auth().signInAnonymously()
      return true
    }

    return false
  }

  func resetSession() async throws {
    UserDefaultsManager.shared.set(false, for: DefaultsKey.hasLaunchedAppBefore)
  }

  func signIn(withCustomToken token: String) async throws {
    try await Auth.auth().signIn(withCustomToken: token)
  }

  func signOut() throws {
    try Auth.auth().signOut()
  }

  func fetchUserIDToken() async throws -> String? {
    do {
      let idToken = try await Auth.auth().currentUser?.getIDToken()
      return idToken
    } catch {
      throw(error)
    }
  }

  func fetchAppCheckToken() async -> String? {
    do {
      let appCheckToken = try await AppCheck.appCheck().token(forcingRefresh: false).token
      return appCheckToken
    } catch {
      print(error)
      return nil
    }
  }

  func fetchCoins(for principal: String) async throws -> Int {
    let doc = try await database.document("users/\(principal)").getDocument()
    return (doc.get("coins") as? Int) ?? 0
  }

  func update(coins: UInt64, forPrincipal principal: String) async throws {
    let document = database.document("users/\(principal)")
    do {
      _ = try await database.runTransaction { transaction, _ in
        transaction.updateData(["coins": coins], forDocument: document)
      }
    } catch {
      print("sarvesh error: \(error)")
    }
  }

  func fetchDocument<T>(
    path: String,
    checkCache: Bool,
    decodeAs type: T.Type
  ) async throws -> T where T: Decodable {
    if checkCache {
      if let cachedSnap = try? await database.document(path).getDocument(source: .cache) {
        return try cachedSnap.data(as: T.self)
      }
    }

    let doc = database.document(path)
    let snapshot = try await doc.getDocument()
    return try snapshot.data(as: T.self)
  }

  func fetchCollection<T>(
    from path: String,
    orderBy fields: [String]?,
    descending: Bool = false,
    limit: Int? = nil,
    decodeAs type: T.Type
  ) async throws -> [T] where T: Decodable {
    var query: Query = database.collection(path)
    if let fields {
      query = query.order(by: FieldPath(fields), descending: descending)
    }

    if let limit {
      query = query.limit(to: limit)
    }

    let snapshot = try await query.getDocuments()
    return try snapshot.documents.compactMap { try $0.data(as: T.self) }
  }

  func documentExists(for path: String) async throws -> Bool {
    let doc = database.document(path)
    return try await doc.getDocument().exists
  }
}
