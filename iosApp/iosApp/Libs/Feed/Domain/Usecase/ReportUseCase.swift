//
//  ReportUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Combine

protocol ReportFeedsUseCaseProtocol {
  func execute(request: ReportRequest) async -> Result<String, FeedError>
}

class ReportFeedsUseCase:
  BaseResultUseCase<ReportRequest, String, FeedError>,
  ReportFeedsUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol, crashReporter: CrashReporter) {
    self.feedRepository = feedRepository
    super.init(crashReporter: crashReporter)
  }

  override func runImplementation(_ request: ReportRequest) async -> Result<String, FeedError> {
    await feedRepository.reportVideo(request: request)
  }
}
