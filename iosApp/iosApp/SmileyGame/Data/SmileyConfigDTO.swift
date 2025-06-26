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
  let imageURL: String
  let isActive: Bool
  let clickAnimation: String

  enum CodingKeys: String, CodingKey {
    case id
    case imageURL = "image_url"
    case isActive = "is_active"
    case clickAnimation = "click_animation"
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
                  imageURL: imageURL,
                  isActive: isActive,
                  clickAnimation: clickAnimation)
  }
}
