//
//  GenerateVideoDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct GenerateVideoDTO: Decodable {
  let operationID: String
  let provider: String
  let requestKey: GenerateVideoRequestKeyDTO

  enum CodingKeys: String, CodingKey {
    case operationID = "operation_id"
    case provider
    case requestKey = "request_key"
  }
}

extension GenerateVideoDTO {
  func toDomain() -> GenerateVideoResponse {
    return GenerateVideoResponse(
      operationID: operationID,
      provider: provider,
      requestKey: requestKey.toDomain()
    )
  }
}

struct GenerateVideoRequestKeyDTO: Decodable {
  let counter: Int
  let principal: String

  enum CodingKeys: String, CodingKey {
    case counter, principal
  }
}

extension GenerateVideoRequestKeyDTO {
  func toDomain() -> GenerateVideoRequestKeyResponse {
    return GenerateVideoRequestKeyResponse(
      counter: counter,
      principal: principal
    )
  }
}

struct GenerateVideoErrorDTO: Decodable {
  let providerError: String

  enum CodingKeys: String, CodingKey {
    case providerError = "provider_error"
  }
}

extension GenerateVideoErrorDTO {
  func toDomain() -> GenerateVideoErrorResponse {
    return GenerateVideoErrorResponse(
      providerError: providerError
    )
  }
}
