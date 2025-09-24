//
//  BTCPriceResponseDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 18/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct BtcPriceResponseDto: Codable {
  let conversionRate: Double
  let currencyCode: String

  enum CodingKeys: String, CodingKey {
    case conversionRate = "conversion_rate"
    case currencyCode = "currency_code"
  }
}
