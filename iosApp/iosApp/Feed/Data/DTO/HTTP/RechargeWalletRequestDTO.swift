//
//  RechargeWalletDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 24/07/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct RechargeWalletRequestDTO: Codable {
  let principalID: String

  enum CodingKeys: String, CodingKey {
    case principalID = "principal_id"
  }
}
