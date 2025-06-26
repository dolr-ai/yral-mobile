//
//  DeleteAccountDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 25/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct DeleteAccountDTO: Codable {
  let delegatedIdentityWire: SwiftDelegatedIdentityWire
  enum CodingKeys: String, CodingKey {
    case delegatedIdentityWire = "delegated_identity_wire"
  }
}
