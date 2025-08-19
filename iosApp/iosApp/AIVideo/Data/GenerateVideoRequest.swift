//
//  GenerateVideoRequest.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct GenerateVideoMetaRequest: Encodable {
  var request: GenerateVideoRequest
  var delegatedIdentity: SwiftDelegatedIdentityWire?

  enum CodingKeys: String, CodingKey {
    case request
    case delegatedIdentity = "delegated_identity"
  }
}

extension GenerateVideoMetaRequest {
  func addingDelegatedIdentity(_ identity: SwiftDelegatedIdentityWire) -> GenerateVideoMetaRequest {
    var copy = self
    copy.delegatedIdentity = identity
    return copy
  }

  func addingPrincipal(_ principal: String) -> GenerateVideoMetaRequest {
    var copy = self
    copy.request.userID = principal
    return copy
  }
}

struct GenerateVideoRequest: Encodable {
  let aspectRatio: String
  let durationSeconds: Int
  let generateAudio: Bool
  let image: Bool?
  let modelID: String
  let negativePrompt: String?
  let prompt: String
  let resolution: String?
  let seed: String?
  let tokenType: String
  var userID: String?

  enum CodingKeys: String, CodingKey {
    case aspectRatio = "aspect_ratio"
    case durationSeconds = "duration_seconds"
    case generateAudio = "generate_audio"
    case image
    case modelID = "model_id"
    case negativePrompt = "negative_prompt"
    case prompt
    case resolution
    case seed
    case tokenType = "token_type"
    case userID = "user_id"
  }
}
