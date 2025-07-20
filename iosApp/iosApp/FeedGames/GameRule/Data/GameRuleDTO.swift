//
//  GameRuleDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import FirebaseFirestore

struct GameRuleDTO: Decodable {
  @DocumentID var id: String?
  let name: String?
  let imageURL: String?
  let body: [BodyElement]

  private enum CodingKeys: String, CodingKey {
    case id, name, body
    case imageURL = "thumbnail_url"
  }
}

enum BodyElement: Decodable {
  case text(content: [String], colors: [String], bolds: [Bool])
  case images(urls: [String])

  private enum CodingKeys: String, CodingKey {
    case type, content, colors, bolds
    case urls = "image_urls"
  }

  init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    let type = try container.decode(String.self, forKey: .type)
    switch type {
    case "text":
      let content = try container.decode([String].self, forKey: .content)
      let colors = try container.decode([String].self, forKey: .colors)
      let bolds = try container.decode([Bool].self, forKey: .bolds)
      self = .text(content: content, colors: colors, bolds: bolds)
    case "images":
      let urls = try container.decode([String].self, forKey: .urls)
      self = .images(urls: urls)
    default:
      throw DecodingError.dataCorrupted(
        .init(codingPath: [CodingKeys.type],
              debugDescription: "Unknown body element type \(type)"))
    }
  }
}

extension GameRuleDTO {
  func toDomain() -> GameRuleResponse {
    return GameRuleResponse(
      id: id,
      name: name,
      imageURL: imageURL,
      body: body)
  }
}
