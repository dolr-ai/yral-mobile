//
//  AIVideoProviderDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct AIVideoProviderMetaDTO: Decodable {
  let providers: [AIVideoProviderDTO]

  enum CodingKeys: String, CodingKey {
    case providers
  }
}

extension AIVideoProviderMetaDTO {
  func toDomain() -> AIVideoProviderMetaResponse {
    return AIVideoProviderMetaResponse(
      providers: providers.map { $0.toDomain() }
    )
  }
}

struct AIVideoProviderDTO: Decodable {
  let id: String
  let name: String
  let description: String
  let isAvailable: Bool
  let modelIcon: String
  let defaultDuration: Int
  let defaultAspectRatio: String

  enum CodingKeys: String, CodingKey {
    case id, name, description
    case isAvailable = "is_available"
    case modelIcon = "model_icon"
    case defaultDuration = "default_duration"
    case defaultAspectRatio = "default_aspect_ratio"
  }
}

extension AIVideoProviderDTO {
  func toDomain() -> AIVideoProviderResponse {
    return AIVideoProviderResponse(
      id: id,
      name: name,
      description: description,
      isActive: isAvailable,
      iconURL: modelIcon,
      defaultDuration: defaultDuration,
      defaultAspectRatio: defaultAspectRatio
    )
  }
}
