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
  let cost: AIVideoProviderCostDTO
  let description: String
  let isAvailable: Bool
  let modelIcon: String
  let supportsAudio: Bool
  let defaultDuration: Int
  let defaultAspectRatio: String

  enum CodingKeys: String, CodingKey {
    case id, name, cost, description
    case isAvailable = "is_available"
    case modelIcon = "ios_model_icon"
    case supportsAudio = "supports_audio"
    case defaultDuration = "default_duration"
    case defaultAspectRatio = "default_aspect_ratio"
  }
}

extension AIVideoProviderDTO {
  func toDomain() -> AIVideoProviderResponse {
    return AIVideoProviderResponse(
      id: id,
      name: name,
      cost: cost.toDomain(),
      description: description,
      isActive: isAvailable,
      iconURL: modelIcon,
      supportsAudio: supportsAudio,
      defaultDuration: defaultDuration,
      defaultAspectRatio: defaultAspectRatio
    )
  }
}

struct AIVideoProviderCostDTO: Decodable {
  let sats: Int

  enum CodingKeys: String, CodingKey {
    case sats
  }
}

extension AIVideoProviderCostDTO {
  func toDomain() -> AIVideoProviderCostResponse {
    return AIVideoProviderCostResponse(
      sats: sats
    )
  }
}
