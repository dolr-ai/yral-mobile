//
//  VideoInsightsUseCase.swift
//  iosApp
//
//  Created by Samarth Paboowal on 29/10/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol VideoInsightsUseCaseProtocol {
  func execute(request: VideoInsightsRequestDTO) async -> Result<[VideoInsightsDTO], ProfileError>
}

class VideoInsightsUseCase: BaseResultUseCase<VideoInsightsRequestDTO, [VideoInsightsDTO], ProfileError>,
                            VideoInsightsUseCaseProtocol {
  private let repository: ProfileRepositoryProtocol

  init(
    repository: ProfileRepositoryProtocol,
    crashReporter: CrashReporter
  ) {
    self.repository = repository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: VideoInsightsRequestDTO) async ->
  Result<[VideoInsightsDTO], ProfileError> {
    await repository.fetchVideoInsights(request: request)
  }
}
