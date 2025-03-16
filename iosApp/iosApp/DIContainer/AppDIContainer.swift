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
    let client = DefaultAuthClient(
      networkService: HTTPService(),
      crashReporter: crashReporter
    )
    return client
  }()

  lazy var crashReporter: CrashReporter = {
    let firebase = FirebaseCrashlyticsReporter()
    let composite = CompositeCrashReporter(reporters: [firebase])
    return composite
  }()

  func makeFeedDIContainer() -> FeedDIContainer {
    return FeedDIContainer(
      dependencies: FeedDIContainer.Dependencies(
        mlfeedService: mlFeedClient,
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter
      )
    )
  }

  func makeAccountDIContainer() -> AccountDIContainer {
    return AccountDIContainer(
      dependencies: AccountDIContainer.Dependencies(
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter
      )
    )
  }

  func makeUploadDIContainer() -> UploadDIContainer {
    return UploadDIContainer(
      dependencies: UploadDIContainer.Dependencies(
        httpService: HTTPService(baseURLString: appConfiguration.uploadBaseURLString),
        authClient: authClient,
        crashReporter: crashReporter
      )
    )
  }

  func makeProfileDIContainer() -> ProfileDIContainer {
    return ProfileDIContainer(
      dependencies: ProfileDIContainer.Dependencies(
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter
      )
    )
  }
}
