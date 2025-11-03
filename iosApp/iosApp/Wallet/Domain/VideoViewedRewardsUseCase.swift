//
//  VideoViewedRewardsUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 02/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol VideoViewedRewardsUseCaseProtocol {
  func execute() async -> Result<VideoViewedRewardsDTO, WalletError>
}

class VideoViewedRewardsUseCase: BaseResultUseCase<Void, VideoViewedRewardsDTO, WalletError>,
                                 VideoViewedRewardsUseCaseProtocol {
  let walletRepository: WalletRepositoryProtocol

  init(
    walletRepository: WalletRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.walletRepository = walletRepository
    super.init(crashReporter: crashReporter)
  }

  func execute() async -> Result<VideoViewedRewardsDTO, WalletError> {
    await super.execute(request: ())
  }

  override func runImplementation(_ request: Void) async -> Result<VideoViewedRewardsDTO, WalletError> {
    await walletRepository.fetchVideoViewedRewardsConfig()
  }
}
