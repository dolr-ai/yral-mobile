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
}
