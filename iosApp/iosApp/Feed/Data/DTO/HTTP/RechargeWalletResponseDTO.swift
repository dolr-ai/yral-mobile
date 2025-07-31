//
//  RechargeWalletDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct RechargeWalletResponseDTO: Codable {
  let coins: Int

  enum CodingKeys: String, CodingKey {
    case coins
  }
}
