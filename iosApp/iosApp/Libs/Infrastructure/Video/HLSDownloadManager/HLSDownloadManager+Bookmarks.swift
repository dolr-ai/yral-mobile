//
//  HLSDownloadManager+Bookmarks.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension HLSDownloadManager {
  func storeBookmark(for assetTitle: String, localFileURL: URL) {
    do {
      let bookmarkData = try localFileURL.bookmarkData()
      storedBookmarks[assetTitle] = bookmarkData
      userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
      userDefaults.synchronize()
    } catch {
      crashReporter.recordException(error)
      print("Failed to create bookmark for \(assetTitle): \(error)")
    }
  }

  func removeBookmark(for assetTitle: String) {
    storedBookmarks.removeValue(forKey: assetTitle)
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }

  func resolveBookmarkIfPresent(for assetTitle: String) -> URL? {
    guard let bookmarkData = storedBookmarks[assetTitle] else { return nil }
    var isStale = false
    do {
      let resolvedURL = try URL(resolvingBookmarkData: bookmarkData,
                                bookmarkDataIsStale: &isStale)
      if isStale {
        print("Bookmark data was stale for \(assetTitle)")
      }
      return resolvedURL
    } catch {
      crashReporter.recordException(error)
      print("Failed to resolve bookmark for \(assetTitle): \(error)")
      return nil
    }
  }

  func removeAllBookmarkedAssetsOnLaunch() {
    for (assetTitle, bookmarkData) in storedBookmarks {
      do {
        var stale = false
        let url = try URL(resolvingBookmarkData: bookmarkData, bookmarkDataIsStale: &stale)
        if fileManager.fileExists(atPath: url.path) {
          try fileManager.removeItem(at: url)
          print("Removed leftover HLS file: \(url.lastPathComponent)")
        }
      } catch {
        crashReporter.recordException(error)
        print("Error removing leftover asset for \(assetTitle): \(error)")
      }
      storedBookmarks.removeValue(forKey: assetTitle)
    }
    userDefaults.set(storedBookmarks, forKey: Self.bookmarksKey)
    userDefaults.synchronize()
  }
}
