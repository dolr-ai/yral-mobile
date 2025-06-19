//
//  AppDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation
import GRPC

@MainActor final class AppDIContainer {
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
      firebaseService: FirebaseService(),
      crashReporter: crashReporter,
      baseURL: URL(string: appConfiguration.authBaseURLString) ?? URL(fileURLWithPath: ""),
      satsBaseURL: URL(string: appConfiguration.satsBalanceBaseURLString) ?? URL(fileURLWithPath: ""),
      firebaseBaseURL: URL(string: appConfiguration.firebaseBaseURLString) ?? URL(fileURLWithPath: "")
    )
    return client
  }()

  lazy var crashReporter: CrashReporter = {
    let firebase = FirebaseCrashlyticsReporter()
    let composite = CompositeCrashReporter(reporters: [firebase])
    return composite
  }()

  lazy var session: SessionManager = SessionManager(auth: authClient)

  lazy var accountsRepository: AccountRepositoryProtocol = {
    AccountRepository(
      httpService: HTTPService(),
      authClient: authClient)
  }()

  lazy var accountUseCase: AccountUseCaseProtocol = {
    AccountUseCase(
      accountRepository: accountsRepository,
      crashReporter: crashReporter
    )
  }()

  lazy var socialSignInUseCase: SocialSignInUseCaseProtocol = {
    SocialSignInUseCase(
      accountRepository: accountsRepository,
      crashReporter: crashReporter
    )
  }()

  func makeFeedDIContainer() async -> FeedDIContainer {
    return FeedDIContainer(
      dependencies: FeedDIContainer.Dependencies(
        mlfeedService: mlFeedClient,
        httpService: HTTPService(baseURLString: appConfiguration.offchainBaseURLString),
        firebaseService: FirebaseService(),
        authClient: authClient,
        crashReporter: crashReporter,
        socialSignInUseCase: socialSignInUseCase,
        session: session,
        castVoteUseCase: CastVoteUseCase(
          castVoteRepository: CastVoteRepository(
            firebaseService: FirebaseService(),
            httpService: HTTPService(baseURLString: appConfiguration.firebaseBaseURLString),
            authClient: authClient
          ),
          crashReporter: crashReporter
        )
      )
    )
  }

  func makeLeaderboardDIContainer() -> LeaderboardDIContainer {
    return LeaderboardDIContainer(
      dependencies: LeaderboardDIContainer.Dependencies(
        firebaseService: FirebaseService(),
        crashReporter: crashReporter,
        authClient: authClient
      )
    )
  }

  func makeAccountDIContainer() -> AccountDIContainer {
    return AccountDIContainer(
      dependencies: AccountDIContainer.Dependencies(
        httpService: HTTPService(),
        authClient: authClient,
        crashReporter: crashReporter,
        accountUseCase: accountUseCase,
        accountRepository: accountsRepository,
        socialSignInUseCase: socialSignInUseCase
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
        httpService: HTTPService(baseURLString: appConfiguration.offchainBaseURLString),
        authClient: authClient,
        crashReporter: crashReporter,
        accountUseCase: accountUseCase,
        session: session
      )
    )
  }
}
