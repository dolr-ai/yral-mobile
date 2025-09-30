//
//  UpdateSessionDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 23/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct UpdateSessionDTO: Codable {
  var canisterId: String
  var userPrincipal: String

  enum CodingKeys: String, CodingKey {
    case canisterId = "user_canister"
    case userPrincipal = "user_principal"
  }
}
