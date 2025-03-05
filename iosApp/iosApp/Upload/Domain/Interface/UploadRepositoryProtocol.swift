//
//  UploadRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

protocol UploadRepositoryProtocol {
  func fetchUploadUrl() async -> Result<UploadEndpointResponse, VideoUploadError>
  func uploadVideoWithProgress(
    fileURL: URL,
    uploadURLString: String
  ) -> AsyncThrowingStream<Double, Error>
}
