//
//  SatsCoinDTO.swift
//  iosApp
//
//  Created by Samarth Paboowal on 10/06/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

struct SatsCoinDTO: Decodable {
  let balance: String
  let airdropped: String
}

extension SatsCoinDTO {
  func toDomain() -> SatsCoinResponse {
    SatsCoinResponse(
      balance: balance,
      airdropped: airdropped
    )
  }
}
