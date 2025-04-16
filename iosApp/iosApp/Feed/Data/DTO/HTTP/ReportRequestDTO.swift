//
//  ReportRequestDTO.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 28/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

struct ReportRequestDTO: Codable {
  let canisterId: String
  let principal: String
  let postId: UInt64
  let reason: String
  let userCanisterId: String
  let userPrincipal: String
  let videoId: String
  let delegatedIdentityWire: SwiftDelegatedIdentityWire
  let reportMode: String = "Ios"

  enum CodingKeys: String, CodingKey {
    case canisterId = "canister_id"
    case principal = "publisher_principal"
    case postId = "post_id"
    case reason
    case userCanisterId = "user_canister_id"
    case userPrincipal = "user_principal"
    case videoId = "video_id"
    case delegatedIdentityWire = "delegated_identity_wire"
    case reportMode = "report_mode"
  }
}
