//
//  FirebaseLottieManager.swift
//  iosApp
//
//  Created by Samarth Paboowal on 26/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation
import FirebaseStorage

final class FirebaseLottieManager {
  static let shared = FirebaseLottieManager()

  private init() {}

  func data(forPath path: String, completion: @escaping (Result<Data, Error>) -> Void) {
    if let data = YralCache.shared.data(forPath: path) {
      return completion(.success(data))
    }

    let ref = Storage.storage().reference(withPath: path)
    ref.getData(maxSize: Constants.maxBytes) { result in
      switch result {
      case .success(let data):
        YralCache.shared.store(data, forPath: path)
        completion(.success(data))
      case .failure(let error):
        print("Failed to download lottie: \(error.localizedDescription)")
        completion(.failure(error))
      }
    }
  }
}

extension FirebaseLottieManager {
  enum Constants {
    static let maxBytes: Int64 = 4 * 1024 * 1024
  }
}
