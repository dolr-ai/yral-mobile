//
//  ClientCredentialsRequest.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 30/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

struct ClientCredentialsRequest: Encodable {
  let grantType: String = "client_credentials"
  let clientId: String

  enum CodingKeys: String, CodingKey {
    case grantType = "grant_type"
    case clientId = "client_id"
  }
}
