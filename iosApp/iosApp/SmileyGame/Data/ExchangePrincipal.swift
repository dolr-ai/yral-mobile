//
//  ExchangePrincipal.swift
//  iosApp
//
//  Created by Samarth Paboowal on 19/05/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct ExchangePrincipalDTO: Decodable {
  let token: String
}

extension ExchangePrincipalDTO {
  func toDomain() -> ExchangePrincipalResponse {
    return ExchangePrincipalResponse(
      token: token
    )
  }
}
