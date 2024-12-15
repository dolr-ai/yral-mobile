//
//  FeedDIContainer.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

final class FeedDIContainer {
  struct Dependencies {
    let mlfeedService: MlFeed_MLFeedNIOClient
    let httpService: HTTPService
  }

  private let dependencies: Dependencies

  init(dependencies: Dependencies) {
    self.dependencies = dependencies
  }
}
