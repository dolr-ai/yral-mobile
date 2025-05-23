//
//  SmileyDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 15/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SmileyConfigDTO: Decodable {
  let availableSmileys: [SmileyDTO]
  let lossPenalty: Int

  enum CodingKeys: String, CodingKey {
    case availableSmileys = "available_smileys"
    case lossPenalty = "loss_penalty"
  }
}

struct SmileyDTO: Decodable {
  let id: String
  let imageName: String
  let isActive: Bool

  enum CodingKeys: String, CodingKey {
    case id
    case imageName = "image_name"
    case isActive = "is_active"
  }
}

extension SmileyConfigDTO {
  func toDomain() -> SmileyConfig {
    return SmileyConfig(smileys: availableSmileys.map { $0.toDomain() }, lossPenalty: lossPenalty)
  }
}

extension SmileyDTO {
  func toDomain() -> Smiley {
    return Smiley(id: id,
                  imageName: imageName,
                  isActive: isActive)
  }
}
