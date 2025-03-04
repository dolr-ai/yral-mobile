//
//  UploadRepositoryProtocol.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

protocol UploadRepositoryProtocol {
  func fetchUploadUrl() async -> Result<UploadEndpointResponse, VideoUploadError>
}
