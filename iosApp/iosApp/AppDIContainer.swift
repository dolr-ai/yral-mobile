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

  lazy var feedsRepository: FeedsRepository = {
    FeedsRepository(
      httpService: HTTPService(),
      mlClient: mlFeedClient,
      authClient: authClient
    )
  }()

  lazy var accountsRepository: AccountRepository = {
    AccountRepository(
      httpService: HTTPService(),
      authClient: authClient)
  }()

  lazy var moreFeedsUseCase: FetchMoreFeedsUseCase = {
    FetchMoreFeedsUseCase(
      feedRepository: feedsRepository,
      crashReporter: crashReporter
    )
  }()

  lazy var toggleLikeUseCase: ToggleLikeUseCase = {
    ToggleLikeUseCase(
      feedRepository: feedsRepository,
      crashReporter: crashReporter
    )
  }()

  lazy var accountUseCase: AccountUseCase = {
    AccountUseCase(
      accountRepository: accountsRepository,
      crashReporter: crashReporter
    )
  }()

  func makeFeedDIContainer() -> FeedDIContainer {
    return FeedDIContainer(
      dependencies: FeedDIContainer.Dependencies(
        mlfeedService: mlFeedClient,
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter,
        feedsRepository: feedsRepository,
        fetchMoreFeedsUseCase: moreFeedsUseCase,
        toggleLikeUseCase: toggleLikeUseCase
      )
    )
  }

  func makeAccountDIContainer() -> AccountDIContainer {
    return AccountDIContainer(
      dependencies: AccountDIContainer.Dependencies(
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter,
        accountUseCase: accountUseCase
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
        httpService: HTTPService(baseURLString: appConfiguration.profileBaseURLString),
        authClient: authClient,
        crashReporter: crashReporter,
        accountRepository: accountsRepository,
        accountUseCase: accountUseCase
      )
    )
  }
}
