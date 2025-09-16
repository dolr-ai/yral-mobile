//
//  UploadResponseDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct UploadResponseDTO: Codable {
  let message: String?
  let success: Bool
  let data: UploadData
}

struct UploadData: Codable {
  let scheduledDeletion: String
  let uid: String
  let uploadURL: String
  let watermark: Watermark
}

struct Watermark: Codable {
  let created: String
  let downloadedFrom: String?
  let height: Double
  let name: String
  let opacity: Double
  let padding: Double
  let position: String
  let scale: Double
  let size: Double
  let uid: String
  let width: Double
}

extension UploadResponseDTO {
  func toDomain() -> UploadEndpointResponse {
    return UploadEndpointResponse(
      url: data.uploadURL,
      videoID: data.uid
    )
  }
}
