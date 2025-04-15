//
//  LikeRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol LikeRepositoryProtocol {
  func toggleLikeStatus(for request: LikeQuery) async -> Result<LikeResult, FeedError>
}
