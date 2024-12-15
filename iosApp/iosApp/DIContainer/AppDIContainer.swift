//
//  AppDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import GRPC

final class AppDIContainer {
  let appConfiguration = AppConfiguration()

  lazy var mlFeedClient: MlFeed_MLFeedNIOClient = {
    let group = PlatformSupport.makeEventLoopGroup(loopCount: .one)
    var configuration = ClientConnection.Configuration.default(
      target: appConfiguration.mlFeedConnectionTarget,
      eventLoopGroup: group
    )
    let tlsConfig = GRPCTLSConfiguration.makeClientConfigurationBackedByNIOSSL(
      certificateChain: [],
      privateKey: nil,
      trustRoots: .default,
      certificateVerification: .fullVerification,
      hostnameOverride: nil,
      customVerificationCallback: nil
    )
    configuration.tlsConfiguration = tlsConfig
    let channel = ClientConnection(configuration: configuration)
    return MlFeed_MLFeedNIOClient(channel: channel)
  }()

  func makeFeedDIContainer() -> FeedDIContainer {
    FeedDIContainer(dependencies: FeedDIContainer.Dependencies(mlfeedService: mlFeedClient, httpService: HTTPService()))
  }
}
