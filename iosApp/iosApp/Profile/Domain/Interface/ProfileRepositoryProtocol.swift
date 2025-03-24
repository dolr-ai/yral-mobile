//
//  ProfileRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 21/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Combine

protocol ProfileRepositoryProtocol {
  func fetchVideos(request: ProfileVideoRequest) async -> Result<[FeedResult], AccountError>
  func refreshVideos() async -> Result<[FeedResult], AccountError>
  func deleteVideo(request: DeleteVideoRequest) async -> Result<Void, AccountError>

  var videosPublisher: AnyPublisher<[FeedResult], Never> { get }
  var newVideosPublisher: AnyPublisher<[FeedResult], Never> { get }
  var deletedVideoPublisher: AnyPublisher<[FeedResult], Never> { get }
}
