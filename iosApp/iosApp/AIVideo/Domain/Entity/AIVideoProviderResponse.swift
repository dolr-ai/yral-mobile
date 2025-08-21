//
//  AIVideoProviderResponse.swift
//  iosApp
//
//  Created by Samarth Paboowal on 13/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct AIVideoProviderMetaResponse {
  let providers: [AIVideoProviderResponse]
}

struct AIVideoProviderResponse: Identifiable {
  let id: String
  let name: String
  let description: String
  let isActive: Bool
  let iconURL: String
  let defaultDuration: Int
  let defaultAspectRatio: String
}
