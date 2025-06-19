//
//  ImageCache.swift
//  iosApp
//
//  Created by Samarth Paboowal on 08/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

final class ImageCache {
  static let shared = ImageCache()
  private let memory = NSCache<NSString, NSData>()
  private let diskURL: URL

  private init() {
    let cachesDir = FileManager.default.urls(for: .cachesDirectory,
                                             in: .userDomainMask).first!
    diskURL = cachesDir.appendingPathComponent("FirebaseImages", isDirectory: true)
    try? FileManager.default.createDirectory(at: diskURL,
                                             withIntermediateDirectories: true)
  }

  // MARK: Public helpers
  func data(forPath path: String) -> Data? {
    if let inMemory = memory.object(forKey: path as NSString) {
      return inMemory as Data
    }

    let fileURL = diskURL.appendingPathComponent(path)
    if let onDisk = try? Data(contentsOf: fileURL) {
      memory.setObject(onDisk as NSData, forKey: path as NSString)
      return onDisk
    }

    return nil
  }

  func store(_ data: Data, forPath path: String) {
    memory.setObject(data as NSData, forKey: path as NSString)

    let fileURL = diskURL.appendingPathComponent(path)
    try? data.write(to: fileURL, options: .atomic)
  }
}
