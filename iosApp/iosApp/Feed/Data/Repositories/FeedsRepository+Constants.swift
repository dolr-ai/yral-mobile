//
//  FeedsRepository+Constants.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 29/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

extension FeedsRepository {
  enum Constants {
    static let cloudfarePrefix = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
    static let cloudflareSuffix = "/manifest/video.m3u8"
    static let thumbnailSuffix = "/thumbnails/thumbnail.jpg"
    static let feedsBaseURL = "https://recommendation-service-82502260393.us-central1.run.app"
    static let cacheSuffix = "/v3/recommendations/cache"
    static let mlFeedSuffix = "/v3/recommendations"
    static let reportVideoPath = "/api/v2/posts/report"
    static let videoEventPath = "/api/v2/events/bulk"
    static let rechargePath = "tap_to_recharge"
    static let initialNumResults: Int64 = 20
    static let mlNumResults: Int64 = 10
    static var firebaseBaseURLString: String {
      guard let uri = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_CLOUD_BASE_URL") as? String,
            !uri.isEmpty
      else {
        return "https://us-central1-yral-mobile.cloudfunctions.net/"
      }
      return uri
    }
  }
}
