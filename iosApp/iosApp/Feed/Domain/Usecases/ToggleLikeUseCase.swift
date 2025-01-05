//
//  ToggleLikeUseCase.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 05/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol ToggleLikeUseCaseProtocol: AnyObject {
  typealias PostID = Int
  func execute(request: PostID) async throws -> Result<Bool, Error>
}

class ToggleLikeUseCase: ToggleLikeUseCaseProtocol {
  private let feedRepository: FeedRepositoryProtocol

  init(feedRepository: FeedRepositoryProtocol) {
    self.feedRepository = feedRepository
  }

  func execute(request: Int) async throws -> Result<Bool, Error> {
    await feedRepository.toggleLikeStatus(for: request)
  }
}
