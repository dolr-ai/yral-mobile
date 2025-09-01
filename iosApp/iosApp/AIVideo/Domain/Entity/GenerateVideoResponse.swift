//
//  GenerateVideoResponse.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct GenerateVideoResponse {
  let operationID: String
  let provider: String
  let requestKey: GenerateVideoRequestKeyResponse
}

struct GenerateVideoRequestKeyResponse {
  let counter: Int
  let principal: String
}

struct GenerateVideoErrorResponse {
  let providerError: String
}
