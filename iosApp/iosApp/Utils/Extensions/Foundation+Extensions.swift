//
//  Foundation+Extensions.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 07/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension Collection {
  subscript(safe index: Index) -> Element? {
    return indices.contains(index) ? self[index] : nil
  }
}

extension Array where Element == FeedResult {
  func deduplicating(_ newFeeds: [FeedResult]) -> [FeedResult] {
    let existingPostIds = Set(self.map { $0.postID })
    let uniqueNewFeeds = newFeeds.filter { !existingPostIds.contains($0.postID) }
    return self + uniqueNewFeeds
  }
}

extension URL: Identifiable {
  public var id: String { absoluteString }
}

extension Encodable {
  func prettyPrintedJSON() throws -> String {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
    return String(data: try encoder.encode(self), encoding: .utf8)!
  }

  func formURLEncoded() throws -> Data {
    let jsonData = try JSONEncoder().encode(self)
    let dict = try JSONSerialization.jsonObject(with: jsonData) as? [String: Any] ?? [:]
    let query = dict.map { key, value in
      let name = key.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
      let val = "\(value)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
      return "\(name)=\(val)"
    }
      .joined(separator: "&")
    return Data(query.utf8)
  }
}
