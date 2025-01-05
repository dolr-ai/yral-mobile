//
//  FeedRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 15/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//

protocol FeedRepositoryProtocol {
  func fetchFeed(request: FeedRequest) async -> Result<[FeedResult], Error>
  func toggleLikeStatus(for postId: Int) async -> Result<Bool, Error>
}
