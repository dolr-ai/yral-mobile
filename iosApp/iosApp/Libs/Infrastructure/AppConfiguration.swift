//
//  AppConfiguration.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import GRPC

final class AppConfiguration {
  let mlFeedConnectionTarget = ConnectionTarget.hostAndPort("yral-ml-feed-server.fly.dev", 443)
  let uploadBaseURLString = "https://yral-upload-video.go-bazzinga.workers.dev/"
  let offchainBaseURLString = "https://icp-off-chain-agent.fly.dev"
  let authBaseURLString = "https://auth.yral.com"
  let anonIdentityBaseURLString = "https://yral.com"
  let satsBalanceBaseURLString = "https://yral-hot-or-not.go-bazzinga.workers.dev/"
  var firebaseBaseURLString: String {
    guard let uri = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_CLOUD_BASE_URL") as? String,
          !uri.isEmpty
    else {
      return "https://us-central1-yral-mobile.cloudfunctions.net/"
    }
    return uri
  }
}
