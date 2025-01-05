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

  lazy var authClient: DefaultAuthClient = {
    let client = DefaultAuthClient(networkService: HTTPService())
    return client
  }()

  func makeFeedDIContainer() -> FeedDIContainer {
    return FeedDIContainer(
      dependencies: FeedDIContainer.Dependencies(
        mlfeedService: mlFeedClient,
        httpService: HTTPService(),
        authClient: authClient
      )
    )
  }

  func makeProfileDIContainer() -> ProfileDIContainer {
    return ProfileDIContainer(
      dependencies: ProfileDIContainer.Dependencies(
        httpService: HTTPService(),
        authClient: authClient
      )
    )
  }
}
